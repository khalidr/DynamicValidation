package com.sample.project.routes

import java.time.ZonedDateTime

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, PathMatcher, PathMatcher1, Route}
import cats.implicits._
import com.sample.project.domain._
import com.sample.project.repo.{FoodUnitRepo, ValidationSetRepo}
import com.sample.project.services.FoodUnitValidator
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.{Json, Reads}

import scala.concurrent.{ExecutionContext, Future}


trait FoodUnitRoutes extends PlayJsonSupport {

  def foodUnitRoutes(foodUnitRepo: FoodUnitRepo, validationSetRepo: ValidationSetRepo)(implicit ec:ExecutionContext): Route =
  {

    def getFoodUnit(id:FoodId):Directive1[FoodUnit] = onSuccess(foodUnitRepo.get(id)).flatMap{
      case Some(f) ⇒ provide(f)
      case None ⇒ complete(NotFound)
    }

    pathPrefix("food-units") {
      pathEndOrSingleSlash {
        post {
          implicit val createReads: Reads[FoodUnit] = FoodUnit.createReads
          entity(as[FoodUnit]) { foodUnit ⇒
            onSuccess(
              {
                for {
                  validations ← validationSetRepo.get(ValidationSetId(foodUnit.productType))
                  validationResults ← Future.successful(FoodUnitValidator(foodUnit.attributes, validations.toList.flatMap(_.validations)))
                  r ←
                    if (validationResults.isValid)
                      foodUnitRepo.insert(foodUnit).map(foodUnit ⇒ Some(foodUnit.id) → ().asRight)
                    else Future.successful(None → validationResults.toEither)
                } yield r
              }
            ) {
              case (Some(id), _) ⇒ complete(Created, Json.obj("id" → id.toString))
              case (None, Left(errors)) ⇒ complete(BadRequest, errors.toList)
              case (None, _) ⇒ complete(InternalServerError, "something went wrong") //should never happen
            }
          }
        }
      } ~
      pathPrefix(FoodIdMatcher) {foodId ⇒
        pathEndOrSingleSlash {
          (put & entity(as[FoodUnit])) { foodUnit =>
            onSuccess(foodUnitRepo.upsert(foodUnit)){ _ ⇒ complete(OK) }
          } ~
          (get & getFoodUnit(foodId)) {
              complete(_)
          } ~
          delete {
            onSuccess(foodUnitRepo.delete(foodId)) {_ ⇒ complete(OK) }
          }
        } ~
        pathPrefix("locations") {
          getFoodUnit(foodId) { foodUnit ⇒
            (post & entity(as[Location])) { location ⇒ // add a new location
              val updated = foodUnit.copy(locations = foodUnit.locations :+ location)
              onSuccess(foodUnitRepo.upsert(updated)) { _ ⇒ complete(OK) }
            } ~
            parameters ('maxResult.?){
              case Some(m) ⇒
                val sorted = foodUnit.locations.sortBy(_.createdDate)(zonedDateTimeOrdering)  //normally we'd offload this sorting to the database
                complete(sorted.take(m.toInt))

              case None ⇒ complete(foodUnit.locations.sortBy(_.createdDate)(zonedDateTimeOrdering))
            }
          }
        }
      }
    }

  }

  val FoodIdMatcher:PathMatcher1[FoodId] = PathMatcher(Segment).map(s ⇒ FoodId(s))

  implicit val zonedDateTimeOrdering: Ordering[ZonedDateTime] = new Ordering[ZonedDateTime] {
    def compare(x: ZonedDateTime, y: ZonedDateTime): Int = x.toEpochSecond.compareTo(y.toEpochSecond)
  }.reverse
}


