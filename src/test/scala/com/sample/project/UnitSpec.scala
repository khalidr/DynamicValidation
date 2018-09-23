package com.sample.project

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Minute, Span}
import org.scalatest.{FlatSpecLike, Matchers, OptionValues}

trait UnitSpec extends FlatSpecLike with OptionValues with Matchers with ScalaFutures {
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(1, Minute)))
}
