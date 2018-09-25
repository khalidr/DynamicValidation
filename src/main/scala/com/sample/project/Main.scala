package com.sample.project

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.couchbase.client.java.{AsyncBucket, CouchbaseAsyncCluster}
import com.sample.project.repo.{FoodUnitRepo, ValidationSetRepo}
import com.sample.project.routes.{FoodUnitRoutes, ValidationSetRoutes}
import com.typesafe.scalalogging.StrictLogging
import rx.lang.scala.JavaConverters._
import rx.lang.scala.Observable

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main extends App with FoodUnitRoutes with ValidationSetRoutes with StrictLogging {

  val cluster: CouchbaseAsyncCluster = CouchbaseAsyncCluster.create("localhost")
  val bucket: Observable[AsyncBucket] = cluster.openBucket("default", "").asScala

  val validationRepo: ValidationSetRepo = new ValidationSetRepo(bucket)
  val foodUnitRepo: FoodUnitRepo = new FoodUnitRepo(bucket)

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val routes = foodUnitRoutes(foodUnitRepo, validationRepo) ~ validationSetRoute(validationRepo)

  val serverBinding = Http().bindAndHandle(
    interface = "localhost",
    port = 8080,
    handler = routes)

  serverBinding.foreach(sb ⇒ logger.info("Successfully bound to {}", sb.localAddress))

  sys.addShutdownHook {
    Await.result(cluster.disconnect().asScala.toBlocking.toFuture, 1.minute)

    actorSystem.terminate()

    Await.result(actorSystem.whenTerminated, 1.minute)
    logger.info("Shutdown complete")
  }
}
