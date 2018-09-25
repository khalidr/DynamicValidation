package com.sample.project.services

import cats.implicits._
import com.sample.project.domain.FoodUnitValidation
import com.sample.project.domain.Expression.ValidationResult
import play.api.libs.json.JsValue

object FoodUnitValidator {
  def apply[A](attributes:Map[String, JsValue], validations:Seq[FoodUnitValidation]): ValidationResult = validations.map(_.logic.apply(attributes)).toList.combineAll
}
