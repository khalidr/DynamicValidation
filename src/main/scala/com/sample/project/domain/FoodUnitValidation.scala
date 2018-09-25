package com.sample.project.domain

import cats.data.ValidatedNel
import cats.syntax.validated._
import com.sample.project.repo.{IdWrites, Identifiable}
import play.api.libs.json.Reads._
import play.api.libs.json._
import Operation._
import com.sample.project.domain.Expression.ValidationResult

sealed trait Expression {
  type A
  implicit def operandWrites:Writes[A]

  def attributeName:String
  def operand:A
  def operation:Operation
  def execute(input:Map[String, JsValue]): ValidationResult

  def operandJson: JsValue = operandWrites.writes(operand)

  def apply(input: Map[String, JsValue]):ValidationResult = execute(input)
}

object Expression {

  type ValidationResult = ValidatedNel[ValidationError, Unit]


  private def extractValue[A:Reads](attributeName:String, input:Map[String, JsValue]):JsResult[A] = input.get(attributeName).map(_.validate[A]).getOrElse(JsError(s"Attribute '$attributeName' not found"))

  def validate[A:Reads](attributeName:String, operand:A, operation:Operation, input:Map[String, JsValue], check: A ⇒ Boolean):ValidationResult = {
    val result =  for {
      attrVal ← extractValue[A](attributeName, input)
      validated ←  JsSuccess(if(check(attrVal)) ().validNel else ValidationError(s"Attribute '$attributeName' with value $attrVal was not '${operation.value}' $operand").invalidNel)
    }yield validated


    result match {
      case JsSuccess(validated, _) ⇒ validated
      case JsError(error) ⇒ ValidationError(error.toString()).invalidNel[Unit]
    }
  }


  case class StringEq(attributeName:String, operand:String) extends Expression {
    type A = String
    val operandWrites: Writes[String] = implicitly[Writes[A]] //we really shouldn't need to do this but just going to hack for now
    val operation: Operation = Operation.$eq

    def execute(input: Map[String, JsValue]): ValidationResult= validate[A](attributeName, operand, operation, input, { attrValue:String ⇒ attrValue == operand})
  }

  case class NumericEq(attributeName:String, operand: BigDecimal) extends Expression {
    type A = BigDecimal
    val operandWrites: Writes[A] = implicitly[Writes[A]]
    val operation: Operation = Operation.$eq
    def execute(input: Map[String, JsValue]): ValidationResult = validate[A](attributeName, operand, operation, input, { attrValue:BigDecimal ⇒ attrValue == operand})
  }

  case class NumericGreaterThan(attributeName:String, operand: BigDecimal) extends Expression {
    type A = BigDecimal
    val operandWrites: Writes[A] = implicitly[Writes[A]]
    val operation: Operation = $gt
    def execute(input: Map[String, JsValue]): ValidationResult = validate[A](attributeName, operand, operation, input, { attrValue:BigDecimal ⇒ attrValue > operand})
  }

  case class NumericLessThan(attributeName:String, operand: BigDecimal) extends Expression {
    type A = BigDecimal
    val operandWrites: Writes[A] = implicitly[Writes[A]]
    val operation: Operation = $lt
    def execute(input: Map[String, JsValue]): ValidationResult = validate[A](attributeName, operand, operation, input, { attrValue:BigDecimal ⇒ attrValue < operand})
  }

  case class NumericGreaterThanEq(attributeName:String, operand: BigDecimal) extends Expression {
    type A = BigDecimal
    val operandWrites: Writes[A] = implicitly[Writes[A]]
    val operation: Operation = $gte
    def execute(input: Map[String, JsValue]): ValidationResult = validate[A](attributeName, operand, operation, input, { attrValue:BigDecimal ⇒ attrValue >= operand})
  }

  case class NumericLessThanEq(attributeName:String, operand: BigDecimal) extends Expression {
    type A = BigDecimal
    val operandWrites: Writes[A] = implicitly[Writes[A]]
    val operation: Operation = $lte
    def execute(input: Map[String, JsValue]): ValidationResult = validate[A](attributeName, operand, operation, input, { attrValue:BigDecimal ⇒ attrValue <= operand})
  }

  def parseLogic[A](target:String, jsValue: JsValue):JsResult[Expression] =

    for {
      jsObj ← jsValue.validate[JsObject]
      logic ← jsObj.value.head match {
        case ("$eq", j@JsString(str)) ⇒ JsSuccess(StringEq(target, str))
        case ("$eq", JsNumber(number)) ⇒ JsSuccess(NumericEq(target, number))
        case ("$gt", JsNumber(number)) ⇒ JsSuccess(NumericGreaterThan(target, number))
        case ("$lt", JsNumber(number)) ⇒ JsSuccess(NumericLessThan(target, number))
        case ("$gte", JsNumber(number)) ⇒ JsSuccess(NumericGreaterThanEq(target, number))
        case ("$lte", JsNumber(number)) ⇒ JsSuccess(NumericLessThanEq(target, number))
        case _ ⇒ JsError("Unsupported operation")
      }
    } yield logic


  def reads(targetAttribute:String):Reads[Expression] = Reads { json ⇒ parseLogic(targetAttribute, json)}
}

case class FoodUnitValidation(logic:Expression)

object FoodUnitValidation {

  implicit def reads:Reads[FoodUnitValidation] =
    Reads[FoodUnitValidation]{json ⇒
      for {
        attr ← (json \ "targetAttribute").validate[String]
        logic ← {
          implicit val reads: Reads[Expression] = Expression.reads(attr)
          (json \ "validation").validate[Expression]
        }
      } yield FoodUnitValidation(logic)

    }

  implicit def writes:Writes[FoodUnitValidation] = Writes{ unit ⇒
    Json.obj("targetAttribute" → unit.logic.attributeName,
    "validation" → {
      Json.obj(unit.logic.operation.value → Json.toJson(unit.logic.operandJson))
    })
  }
}

case class ValidationError(msg:String)

object ValidationError {
  implicit val formats:OFormat[ValidationError] = Json.format[ValidationError]
}


case class ValidationSetId(value:String) {
  override def toString: String = value
}

object ValidationSetId {
  implicit val idWrites:IdWrites[ValidationSetId] = (id: ValidationSetId) ⇒ id.value

  implicit val formats:OFormat[ValidationSetId] = Json.format[ValidationSetId]
}

case class ValidationSet(id:ValidationSetId, validations:Seq[FoodUnitValidation]) extends Identifiable[ValidationSetId]

object ValidationSet {
  implicit def format:Format[ValidationSet] = Json.format[ValidationSet]

}
