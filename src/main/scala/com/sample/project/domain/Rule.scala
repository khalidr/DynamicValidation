package com.sample.project.domain

import com.sample.project.repo.{IdWrites, Identifiable}
import play.api.libs.json.{Json, OFormat}

case class RuleId(value:String)

object RuleId {
  implicit val idWrites:IdWrites[RuleId] = (id: RuleId) ⇒ id.value

  implicit val formats:OFormat[RuleId] = Json.format[RuleId]
}

case class Rule (id:RuleId, name:String) extends Identifiable[RuleId]

object Rule {
  implicit val formats:OFormat[Rule] = Json.format[Rule]
}