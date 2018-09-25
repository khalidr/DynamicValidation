package com.sample.project.domain

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID

import com.sample.project.repo.{IdWrites, Identifiable}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._

case class FoodId(value: String = UUID.randomUUID().toString) {
  override def toString: String = value
}

object FoodId {

  implicit val idWrites: IdWrites[FoodId] = (id: FoodId) ⇒ id.value
  implicit val formats: OFormat[FoodId] = Json.format[FoodId]
}

case class Location(longitude: Double, latitude: Double, createdDate:ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)) // normally I'd use something like squants to properly represent a measurement with the proper units

object Location {
  implicit val reads: Reads[Location] =
  {
    (
      (__ \ "longitude").read[Double] ~
        (__ \ "latitude").read[Double] ~
        (__ \ "createdDate").readNullable[ZonedDateTime].map(_.getOrElse(ZonedDateTime.now(ZoneOffset.UTC)))
      )(Location.apply _)
  }

  implicit val writes:Writes[Location] = Json.writes[Location]
}

case class FoodUnit(owner: String,
                    productType: String,
                    unitDescription: String,
                    mass: Double, // here too.  We should use something that takes the units into account.
                    expiryDate: ZonedDateTime,
                    attributes: Map[String, JsValue] = Map.empty, //limiting to JsValue now but this may not be necessary.  Maybe we should create our own types.
                    locations:Seq[Location] = Seq.empty,
                    createdDate: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
                    id: FoodId = FoodId()) extends Identifiable[FoodId]

object FoodUnit {
  implicit val formats: OFormat[FoodUnit] = Json.format[FoodUnit]

  val createReads:Reads[FoodUnit] =
    (
      (__ \ "owner").read[String] ~
      (__ \ "productType").read[String] ~
      (__ \ "unitDescription").read[String] ~
      (__ \ "mass").read[Double] ~
      (__ \ "expiryDate").read[ZonedDateTime] ~
      (__ \ "attributes").readNullable[JsObject].map(opt ⇒ opt.toList.flatMap(_.value).toMap) ~
      (__ \ "locations").readNullable[Seq[Location]].map(_.toSeq.flatten) ~
      (__ \ "createdDate").readNullable[ZonedDateTime].map(_.getOrElse(ZonedDateTime.now(ZoneOffset.UTC))) ~
      (__ \ "id").readNullable[FoodId].map(_.getOrElse(FoodId()))
    )(FoodUnit.apply _)

}

