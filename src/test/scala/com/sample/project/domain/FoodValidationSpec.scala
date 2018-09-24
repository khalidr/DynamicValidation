package com.sample.project.domain

import cats.data.Validated.Invalid
import com.sample.project.domain.Logic.NumericEq
import com.sample.project.{UnitSpec, domain}
import play.api.libs.json.Json
import cats.syntax.validated._

class FoodValidationSpec extends UnitSpec {

  "FoodValidation" should "be parsed json correctly" in {
    val validationJson = Json.obj(
      "targetAttribute" → "foo",
      "validation" → Json.obj("$eq" → 1))

    val input = Json.obj("foo" → 1, "bar" → 2)

    val notEqualInput = Json.obj("foo" → 2, "bar" → 2)

    val validation = validationJson.as[FoodUnitValidation[BigDecimal]]
    validation shouldBe FoodUnitValidation(NumericEq("foo", 1))

    validation.logic(input.value.toMap) shouldBe 1.validNel[ValidationError]

    val result = validation.logic(notEqualInput.value.toMap)
    result shouldBe a[Invalid[ValidationError]]
  }
}
