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

import scala.concurrent.{ExecutionContext, Future}

//@formatter:off
trait FoodUnitRoutes extends PlayJsonSupport {

  def foodUnitRoutes(foodUnitRepo: FoodUnitRepo, validationSetRepo: ValidationSetRepo)(implicit ec:ExecutionContext): Route =
  {

    def getFoodUnit(id:FoodId):Directive1[FoodUnit] = onSuccess(foodUnitRepo.get(id)).flatMap{
      case Some(f) ⇒ provide(f)
      case None ⇒ complete(NotFound)
    }

    pathPrefix("food-units") {
      pathEndOrSingleSlash {
        (post & entity(as[FoodUnit])) { foodUnit ⇒
            onSuccess(
              {
                  for {
                    validations ← validationSetRepo.get(ValidationSetId(foodUnit.productType))
                  validationResults ← Future.successful(FoodUnitValidator(foodUnit.attributes, validations.toList.flatMap(_.validations)))
                  r ←
                    if (validationResults.isValid)
                      foodUnitRepo.insert(foodUnit).map(_ ⇒ ().validNel)
                    else Future.successful(validationResults)
                } yield r.toEither
              }
            ){
              case Right(_) ⇒ complete(Created)
              case Left(errors) ⇒ complete(BadRequest, errors.toList )}
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

              case None ⇒ complete(foodUnit.locations)
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


