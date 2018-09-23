package com.sample.project

import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestKit

import concurrent.duration._

trait RouteSpec extends UnitSpec with ScalatestRouteTest {

  implicit val routeTestTimeout: RouteTestTimeout = RouteTestTimeout(2.minutes)

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }
}
