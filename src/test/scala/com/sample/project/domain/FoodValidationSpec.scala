package com.sample.project.domain

import cats.data.Validated.Invalid
import cats.syntax.validated._
import com.sample.project.UnitSpec
import com.sample.project.domain.Expression.NumericEq
import play.api.libs.json.Json

class FoodValidationSpec extends UnitSpec {

  "FoodValidation" should "be parsed json correctly" in {
    val validationJson = Json.obj(
      "targetAttribute" → "foo",
      "validation" → Json.obj("$eq" → 1))

    val input = Json.obj("foo" → 1, "bar" → 2)

    val notEqualInput = Json.obj("foo" → 2, "bar" → 2)

    val validation = validationJson.as[FoodUnitValidation]
    validation shouldBe FoodUnitValidation(NumericEq("foo", 1))

    validation.logic(input.value.toMap) shouldBe ().validNel[ValidationError]

    val result = validation.logic(notEqualInput.value.toMap)
    result shouldBe a[Invalid[_]]
  }

  it should "be serializable to json" in {
    val validation = FoodUnitValidation(NumericEq("foo", 1))
    val expectedJson = Json.obj("targetAttribute" → "foo",
    "validation" → Json.obj("$eq" → 1))


    Json.toJson(validation) shouldBe expectedJson
  }

  it should "work" in {

    val json = Json.obj(
      "owner" → "delmonte",
      "productType" → "carrots",
      "unitDescription" → "carrots",
      "mass" →  1.1,
      "expiryDate" → "2018-09-25T03:45:49.788Z",
      "createdDate" → "2018-09-25T03:45:49.788Z",
      "manufactorId" → 12345,
      "kind" → "orange"
    )


    println(Json.fromJson[FoodUnit](json))


  }
}
