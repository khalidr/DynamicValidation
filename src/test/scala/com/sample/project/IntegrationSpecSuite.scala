package com.sample.project

import com.sample.project.repo.FoodUnitRepoSpec
import com.sample.project.routes.{FoodUnitRoutesSpec, ValidationSetRouteSpec}
import org.scalatest.Suite

class IntegrationSpecSuite extends SpecSuite {

  /**
    * Implement this method with the suites to run.
    *
    * @return
    */
  def suites: Vector[Suite] =
    Vector(
      new FoodUnitRepoSpec(bucket),
      new FoodUnitRoutesSpec(bucket),
      new ValidationSetRouteSpec(bucket)
    )
}
