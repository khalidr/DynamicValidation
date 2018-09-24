package com.sample.project.routes

import java.time.{ZoneOffset, ZonedDateTime}

import akka.http.scaladsl.server.Route
import com.couchbase.client.java.AsyncBucket
import com.sample.project.RouteSpec
import com.sample.project.domain.{FoodId, FoodUnit, Location}
import com.sample.project.repo.FoodUnitRepo
import rx.lang.scala.Observable
import akka.http.scaladsl.model.StatusCodes._
import org.scalatest.DoNotDiscover

@DoNotDiscover
class FoodUnitRoutesSpec(val bucket: Observable[AsyncBucket]) extends RouteSpec with FoodUnitRoutes {

  val foodUnitRepo: FoodUnitRepo = new FoodUnitRepo(bucket)
  val rootUrl = "/food-units"

  "FoodUnitRoute" should "create a foodUnit" in new Context {
    val unit = FoodUnit("delmonte", "carrots", "carrots", 1.1, ZonedDateTime.now(ZoneOffset.UTC))
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

  trait Context {
    val route: Route = foodUnitRoutes(foodUnitRepo)
  }

}
