package com.sample.project.routes

import com.couchbase.client.java.AsyncBucket
import com.sample.project.RouteSpec
import com.sample.project.domain.FoodUnit
import com.sample.project.repo.FoodUnitRepo
import rx.lang.scala.Observable

class FoodUnitRoutesSpec(val bucket:Observable[AsyncBucket]) extends RouteSpec with FoodUnitRoutes{

  val foodUnitRepo:FoodUnitRepo = new FoodUnitRepo(bucket)

  "FoodUnitRoute" should "create a foodUnit" in {
      val unit = FoodUnit()
  }

}
