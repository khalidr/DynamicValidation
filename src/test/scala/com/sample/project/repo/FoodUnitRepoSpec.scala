package com.sample.project.repo

import java.time.ZonedDateTime

import com.couchbase.client.java.AsyncBucket
import com.sample.project.UnitSpec
import com.sample.project.domain.FoodUnit
import org.scalatest.DoNotDiscover
import play.api.libs.json.JsString
import rx.lang.scala.Observable

import scala.concurrent.ExecutionContext.Implicits.global

@DoNotDiscover
class FoodUnitRepoSpec(val bucket:Observable[AsyncBucket]) extends UnitSpec {

  val repo = new FoodUnitRepo(bucket)

  "FoodUnitRepo" should "perform CRUD operations" in {

    val carrots = FoodUnit(owner = "Delmonte", productType = "Carrots", unitDescription = "foobar", mass = 1.1, expiryDate = ZonedDateTime.now(), attributes = Map("foo" → JsString("abc")))
    val updatedCarrots = carrots.copy(mass = 2.0)

    whenReady(for {
      i ← repo.insert(carrots)
      get ← repo.get(i.id)
      _ ← repo.upsert(updatedCarrots)
      updated ← repo.get(carrots.id)
    } yield (get, updated)){ case (get, updated) ⇒
        get.value shouldBe carrots
        updated.value shouldBe updatedCarrots
    }

    whenReady(
      for{
        _ ← repo.delete(carrots.id)
        result ← repo.get(carrots.id)
      } yield result
    ) { _ shouldBe empty}

  }
}
