package com.sample.project.domain

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID

import com.sample.project.repo.{IdWrites, Identifiable}
import play.api.libs.json.{JsValue, Json, OFormat}

case class FoodId(value: String = UUID.randomUUID().toString) {
  override def toString: String = value
}

object FoodId {

  implicit val idWrites: IdWrites[FoodId] = (id: FoodId) ⇒ id.value
  implicit val formats: OFormat[FoodId] = Json.format[FoodId]
}

case class Location(longitude: Double, latitude: Double, createdDate:ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)) // normally I'd use something like squants to properly represent a measurement with the proper units

object Location {
  implicit val formats:OFormat[Location] = Json.format[Location]
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

}

