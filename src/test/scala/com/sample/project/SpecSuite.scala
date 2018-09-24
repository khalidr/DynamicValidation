package com.sample.project

import java.lang

import com.couchbase.client.java.{AsyncBucket, CouchbaseAsyncCluster}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Minute, Seconds, Span}
import rx.lang.scala.JavaConverters._
import rx.lang.scala.Observable

import scala.util.{Failure, Success, Try}

trait SpecSuite
  extends Suite
    with ScalaFutures
    with BeforeAndAfterAll
    with LazyLogging{


  val cluster: CouchbaseAsyncCluster = CouchbaseAsyncCluster.create("localhost")
  val bucket: Observable[AsyncBucket] = cluster.openBucket("default", "").asScala

  /**
    * Implement this method with the suites to run.
    * @return
    */
  def suites:Vector[Suite]

  /**
    * Ensure that there are no exception when instantiating the Specs.  If there are, first delete the bucket and disconnect before stopping test execution.
    * @return
    */
  override val nestedSuites: Vector[Suite] = Try(suites) match {
    case Success(s) => s
    case Failure(f) =>
      disconnect()
      throw f
  }

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(1, Minute)), interval = scaled(Span(1, Seconds)))

  override def afterAll(): Unit = {
    try super.afterAll()
    finally disconnect()
  }

  def disconnect(): lang.Boolean = {
    cluster.disconnect().toBlocking.first()
  }
}