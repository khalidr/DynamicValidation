package com.sample.project.domain

import cats.data
import cats.data.{NonEmptyList, ValidatedNel}
import com.sample.project.repo.IdWrites
import play.api.libs.json.Reads._
import play.api.libs.json._
import cats.syntax.validated._


case class ValidationId(value:String)

object ValidationId {
  implicit val idWrites:IdWrites[ValidationId] = (id: ValidationId) ⇒ id.value

  implicit val formats:OFormat[ValidationId] = Json.format[ValidationId]
}

sealed trait Logic{
  type A
  type ValidationResult = ValidatedNel[ValidationError, A]

  /*def attributeName:String
  def input:Map[String, JsValue]*/
  def value(input:Map[String, JsValue]): ValidationResult
}

object Logic {

  private def extractValue[A:Reads](attributeName:String, input:Map[String, JsValue]):JsResult[A] = input.get(attributeName).map(_.validate[A]).getOrElse(JsError(s"Attribute $attributeName not found"))


  case class StringEq(attributeName:String, operand:String) extends Logic {
    type A = String
    def value(input: Map[String, JsValue]): ValidationResult = {

      val result =  for {
        attrVal ← extractValue[String](attributeName, input)
        validated ←  JsSuccess(if(attrVal == operand) attrVal.validNel else ValidationError(s"Attribute $attributeName with value $attrVal was not '$$eq' $operand").invalidNel)
      }yield validated


      result match {
        case JsSuccess(validated, _) ⇒ validated
        case JsError(error) ⇒ ValidationError(error.toString()).invalidNel[A]
      }
    }
  }

  /*case class NumericEq(attributeName:String, equalTo: BigDecimal) extends Logic {
    type A = BigDecimal

    def value(input: Map[String, JsValue]): ValidationResult = {

      val result =  for {
        attrVal ← extractValue[String](attributeName, input)
        validated ←  JsSuccess(if(attrVal == operand) attrVal.validNel else ValidationError(s"Attribute $attributeName with value $attrVal was not '$$eq' $operand").invalidNel)
      }yield validated


      result match {
        case JsSuccess(validated, _) ⇒ validated
        case JsError(error) ⇒ ValidationError(error.toString()).invalidNel[A]
      }
    }
  }*/


  def parseLogic[A](target:String, jsValue: JsValue):JsResult[Logic] =

    for {
      jsObj ← jsValue.validate[JsObject]
      logic ← jsObj.value.head match {
        case ("$eq", j@JsString(str)) ⇒ JsSuccess(StringEq(target, str))
      //  case ("$eq", JsNumber(number)) ⇒ JsSuccess(NumericEq(number))
        case _ ⇒ JsError("Unsupported operation")
      }
    } yield logic


  def reads(targetAttribute:String):Reads[Logic] = Reads {json ⇒ parseLogic(targetAttribute, json)}
}

/*
*/

case class FoodUnitValidation[A](attributeName:String, logic:Logic)

object FoodUnitValidation {

  implicit def formats[A]:Reads[FoodUnitValidation[A]] =
    Reads[FoodUnitValidation[A]]{json ⇒
      for {
        attr ← (json \ "targetAttribute").validate[String]
        logic ← {
          implicit val reads: Reads[Logic] = Logic.reads(attr)
          (json \ "validation").validate[Logic]
        }
      } yield FoodUnitValidation(attr, logic)

    }

}

case class ValidationError(msg:String)

/*
object ValidationError {
  def apply(attributeName:String, operation:String,  attrValue:Option[String] = None) = new ValidationError{val msg = attrValue.map(v ⇒ s"Attribute $attributeName with value $v was not '$$eq' $operand")}
}*/
