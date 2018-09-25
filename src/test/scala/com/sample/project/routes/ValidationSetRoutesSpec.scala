package com.sample.project.routes

import com.couchbase.client.java.AsyncBucket
import com.sample.project.RouteSpec
import com.sample.project.domain.Expression.{NumericEq, StringEq}
import com.sample.project.domain.{FoodUnitValidation, ValidationSet, ValidationSetId}
import com.sample.project.repo.ValidationSetRepo
import org.scalatest.DoNotDiscover
import rx.lang.scala.Observable
import akka.http.scaladsl.model.StatusCodes._


@DoNotDiscover
class ValidationSetRoutesSpec(val bucket: Observable[AsyncBucket]) extends RouteSpec with ValidationSetRoutes {

  val repo = new ValidationSetRepo(bucket)

  "ValidationSetRoute" should "insert and get" in {

    val validationSet = ValidationSet(ValidationSetId("carrots"), List( FoodUnitValidation(NumericEq("manufactureId", 12345)), FoodUnitValidation(StringEq("kind", "orange"))))

    Post("/validations", validationSet) ~> validationSetRoute(repo) ~> check {
      status shouldBe Created
    }

    Get(s"/validations/${validationSet.id}") ~> validationSetRoute(repo) ~> check {
      val r = responseAs[ValidationSet]
      r.id shouldBe validationSet.id
      r.validations should contain theSameElementsAs validationSet.validations
    }

    Delete(s"/validations/${validationSet.id}") ~> validationSetRoute(repo) ~> check {
      status shouldBe OK
      whenReady(repo.get(validationSet.id)){ _ shouldBe empty }
    }
  }
}
