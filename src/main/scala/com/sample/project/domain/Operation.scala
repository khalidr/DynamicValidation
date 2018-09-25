package com.sample.project.domain

sealed trait Operation{
  val value:String
}

object Operation {
  case object $lt extends Operation{val value = "$lt"}
  case object $gt extends Operation{val value = "$gt"}
  case object $gte extends Operation{val value = "$gte"}
  case object $lte extends Operation{val value = "$lte"}
  case object $eq extends Operation{val value = "$eq"}
}
