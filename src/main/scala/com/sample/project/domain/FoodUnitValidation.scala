package com.sample.project.domain

import cats.data.ValidatedNel
import cats.syntax.validated._
import com.sample.project.repo.IdWrites
import play.api.libs.json.Reads._
import play.api.libs.json._


case class ValidationId(value:String)

object ValidationId {
  implicit val idWrites:IdWrites[ValidationId] = (id: ValidationId) ⇒ id.value

  implicit val formats:OFormat[ValidationId] = Json.format[ValidationId]
}

sealed trait Logic{
  type A //there is probably an easier way to do this.  This Generic should be at the trait level
  type ValidationResult = ValidatedNel[ValidationError, A]

  def attributeName:String
  def operand:A
  def value(input:Map[String, JsValue]): ValidationResult

  def apply(input: Map[String, JsValue]):ValidationResult = value(input)
}

object Logic {

  private def extractValue[A:Reads](attributeName:String, input:Map[String, JsValue]):JsResult[A] = input.get(attributeName).map(_.validate[A]).getOrElse(JsError(s"Attribute $attributeName not found"))

  def validate[A:Reads](attributeName:String, operand:A, input:Map[String, JsValue], isValid: A ⇒ Boolean):ValidatedNel[ValidationError, A] = {
    val result =  for {
      attrVal ← extractValue[A](attributeName, input)
      validated ←  JsSuccess(if(isValid(attrVal)) attrVal.validNel else ValidationError(s"Attribute '$attributeName' with value $attrVal was not '$$eq' $operand").invalidNel)
    }yield validated


    result match {
      case JsSuccess(validated, _) ⇒ validated
      case JsError(error) ⇒ ValidationError(error.toString()).invalidNel[A]
    }
  }


  case class StringEq(attributeName:String, operand:String) extends Logic {
    type A = String
    def value(input: Map[String, JsValue]): ValidationResult = validate[A](attributeName, operand, input, { attrValue:String ⇒ attrValue == operand})
  }

  case class NumericEq(attributeName:String, operand: BigDecimal) extends Logic {
    type A = BigDecimal

    def value(input: Map[String, JsValue]): ValidationResult = validate[A](attributeName, operand, input, {attrValue:BigDecimal ⇒ attrValue == operand})
  }

  case class NumericGreaterThan(attributeName:String, operand: BigDecimal) extends Logic {
    type A = BigDecimal

    def value(input: Map[String, JsValue]): ValidationResult = validate[A](attributeName, operand, input, {attrValue:BigDecimal ⇒ attrValue > operand})
  }

  case class NumericLessThan(attributeName:String, operand: BigDecimal) extends Logic {
    type A = BigDecimal

    def value(input: Map[String, JsValue]): ValidationResult = validate[A](attributeName, operand, input, {attrValue:BigDecimal ⇒ attrValue < operand})
  }

  case class NumericGreaterThanEq(attributeName:String, operand: BigDecimal) extends Logic {
    type A = BigDecimal

    def value(input: Map[String, JsValue]): ValidationResult = validate[A](attributeName, operand, input, {attrValue:BigDecimal ⇒ attrValue >= operand})
  }

  case class NumericLessThanEq(attributeName:String, operand: BigDecimal) extends Logic {
    type A = BigDecimal

    def value(input: Map[String, JsValue]): ValidationResult = validate[A](attributeName, operand, input, {attrValue:BigDecimal ⇒ attrValue <= operand})
  }


  def parseLogic[A](target:String, jsValue: JsValue):JsResult[Logic] =

    for {
      jsObj ← jsValue.validate[JsObject]
      logic ← jsObj.value.head match {
        case ("$eq", j@JsString(str)) ⇒ JsSuccess(StringEq(target, str))
        case ("$eq", JsNumber(number)) ⇒ JsSuccess(NumericEq(target, number))
        case ("$gt", JsNumber(number)) ⇒ JsSuccess(NumericGreaterThan(target, number))
        case ("$lt", JsNumber(number)) ⇒ JsSuccess(NumericLessThan(target, number))
        case _ ⇒ JsError("Unsupported operation")
      }
    } yield logic


  def reads(targetAttribute:String):Reads[Logic] = Reads {json ⇒ parseLogic(targetAttribute, json)}
}

case class FoodUnitValidation[A](logic:Logic)

object FoodUnitValidation {

  implicit def formats[A]:Reads[FoodUnitValidation[A]] =
    Reads[FoodUnitValidation[A]]{json ⇒
      for {
        attr ← (json \ "targetAttribute").validate[String]
        logic ← {
          implicit val reads: Reads[Logic] = Logic.reads(attr)
          (json \ "validation").validate[Logic]
        }
      } yield FoodUnitValidation(logic)

    }

}

case class ValidationError(msg:String)
