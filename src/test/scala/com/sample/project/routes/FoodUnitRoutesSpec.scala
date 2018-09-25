package com.sample.project.routes

import java.time.{ZoneOffset, ZonedDateTime}

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import com.couchbase.client.java.AsyncBucket
import com.sample.project.RouteSpec
import com.sample.project.domain.Logic.{NumericEq, NumericGreaterThan, NumericLessThanEq, StringEq}
import com.sample.project.domain._
import com.sample.project.repo.{FoodUnitRepo, ValidationSetRepo}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.DoNotDiscover
import play.api.libs.json.{JsNumber, JsString}
import rx.lang.scala.Observable

@DoNotDiscover
class FoodUnitRoutesSpec(val bucket: Observable[AsyncBucket]) extends RouteSpec with FoodUnitRoutes with PlayJsonSupport {

  val foodUnitRepo: FoodUnitRepo = new FoodUnitRepo(bucket)
  val validationRepo:ValidationSetRepo = new ValidationSetRepo(bucket)

  val rootUrl = "/food-units"

  "FoodUnitRoute" should "create a foodUnit that has no validation rules" in new Context {
    val unit = FoodUnit("delmonte", "carrots", "carrots", 1.1, ZonedDateTime.now(ZoneOffset.UTC), attributes = Map("manufactorId" -> JsNumber(12345), "kind" → JsString("orange")) )
    Post(rootUrl, unit) ~> route ~> check {
      status shouldBe Created
    }

    Get(s"$rootUrl/${unit.id}") ~> route ~> check {
      responseAs[FoodUnit] shouldBe unit
    }

    foodUnitRepo.delete(unit.id).futureValue
  }

  it should "add and get locations" in new Context {

    val foodUnit = FoodUnit("acme", "fish", "fish", 1.1, ZonedDateTime.now(ZoneOffset.UTC), locations = Seq(Location(100,100, ZonedDateTime.now(ZoneOffset.UTC).minusDays(2))))
    val newLocation = Location(50,50)

    //add the food unit
    Post(s"$rootUrl", foodUnit) ~> route ~> check {
      status shouldBe Created
    }

    //add a new location to the food unit
    Post(s"$rootUrl/${foodUnit.id}/locations", newLocation ) ~> route ~> check {
      status shouldBe OK
    }

    //get the locations for the foodunit
    Get(s"$rootUrl/${foodUnit.id}/locations") ~> route ~> check {
      responseAs[List[Location]] should contain theSameElementsAs(foodUnit.locations :+ newLocation)
    }

    //get the latest location
    Get(s"$rootUrl/${foodUnit.id}/locations?maxResult=1") ~> route ~> check {
      responseAs[List[Location]] shouldBe List(newLocation)
    }

    foodUnitRepo.delete(foodUnit.id).futureValue
  }

  it should "create a food unit after passing validation rules" in new Context {
    val validations = ValidationSet(ValidationSetId("parsnips"), List( FoodUnitValidation(NumericEq("manufacturerId", 12345)), FoodUnitValidation(StringEq("kind", "orange")), FoodUnitValidation(NumericLessThanEq("rotten", 2)), FoodUnitValidation(NumericGreaterThan("count", 10))))
    val unit = FoodUnit("delmonte", "carrots", "carrots", 1.1, ZonedDateTime.now(ZoneOffset.UTC), attributes = Map("manufacturerId" -> JsNumber(12345), "kind" → JsString("orange"), "rotten" → JsNumber(0), "count" → JsNumber(20)) )

    whenReady {
      for {
        r ← validationRepo.insert(validations)
      } yield r
    } {_ ⇒
      Post(rootUrl, unit) ~> route ~> check {
        status shouldBe Created
      }

      Get(s"$rootUrl/${unit.id}") ~> route ~> check {
        responseAs[FoodUnit] shouldBe unit
      }
    }

    foodUnitRepo.delete(unit.id).futureValue
    validationRepo.delete(validations.id).futureValue
  }

  it should "not create a food unit which fails validation" in new Context {
    val validations = ValidationSet(ValidationSetId("parsnips"), List( FoodUnitValidation(NumericEq("manufacturerId", 12345)), FoodUnitValidation(StringEq("color", "orange")), FoodUnitValidation(NumericLessThanEq("rotten", 2)), FoodUnitValidation(NumericGreaterThan("count", 10))))
    val unit = FoodUnit("delmonte", "parsnips", "parsnips from Brazil", 1.1, ZonedDateTime.now(ZoneOffset.UTC), attributes = Map("manufacturerId" -> JsNumber(45), "color" → JsString("white"), "rotten" → JsNumber(4), "count" → JsNumber(6)) )

    whenReady {
      for {
        r ← validationRepo.insert(validations)
      } yield r
    } { _ ⇒
      Post(rootUrl, unit) ~> route ~> check {
        status shouldBe BadRequest

        val errors = responseAs[List[ValidationError]]

        val expectedErrors = List(
          "Attribute 'manufacturerId' with value 45 was not '$eq' 12345",
          "Attribute 'color' with value white was not '$eq' orange",
          "Attribute 'rotten' with value 4 was not '$lte' 2",
          "Attribute 'count' with value 6 was not '$gt' 10")

        errors.map(_.msg) should contain theSameElementsAs expectedErrors
      }

      Get(s"$rootUrl/${unit.id}") ~> route ~> check {
        status shouldBe NotFound
      }
    }

    validationRepo.delete(validations.id).futureValue
  }

  trait Context {
    val route: Route = foodUnitRoutes(foodUnitRepo, validationRepo)
  }

}
