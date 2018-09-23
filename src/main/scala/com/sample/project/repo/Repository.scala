package com.sample.project.repo

import com.couchbase.client.java.AsyncBucket
import com.couchbase.client.java.document.RawJsonDocument
import com.sample.project.domain.{FoodId, FoodUnit, Rule, RuleId}
import play.api.libs.json.{Format, Json, Reads, Writes}
import rx.lang.scala.Observable

import scala.annotation.implicitNotFound
import scala.concurrent.{ExecutionContext, Future}
import rx.lang.scala.JavaConverters._

trait Repository [ID, Entity <: Identifiable[ID]]{

  def get(id:ID):Future[Option[Entity]]
  def insert(e:Entity):Future[Entity]
  def upsert(e:Entity):Future[Entity]
  def delete(id:ID):Future[ID]
}

trait Identifiable[A] { def id:A }


trait CouchbaseRepository[ID, Entity <: Identifiable[ID]] extends Repository[ID, Entity]{

  implicit def idWrites:IdWrites[ID]

  protected def bucket:Observable[AsyncBucket]

  implicit def ec:ExecutionContext

  implicit def formats:Format[Entity]

  def get(id: ID): Future[Option[Entity]] = {
    for {
      b ← bucket
      entity ← b.get(id, classOf[RawJsonDocument]).asScala
    } yield Json.parse(entity.content()).as[Entity]
  }.singleOption.toBlocking.toFuture

  def insert(e: Entity): Future[Entity] = {
    for {
      b ← bucket
      inserted ← b.insert(RawJsonDocument.create(e.id, Json.toJson(e).toString())).asScala
    } yield inserted.as[Entity]
  }.toBlocking.toFuture

  def upsert(e: Entity): Future[Entity] = {
    for {
      b ← bucket
      upserted ← b.upsert(e.asDocument).asScala
    }yield upserted.as[Entity]
  }.toBlocking.toFuture

  def delete(id: ID): Future[ID] = {
    for {
      b ← bucket
      _ ← b.remove(id).asScala
    } yield id
  }.toBlocking.toFuture

  implicit def id2String(id:ID):String = idWrites.write(id)

  implicit class RichRawDocument(doc:RawJsonDocument) {
    def as[A:Reads] = Json.parse(doc.content()).as[A]
  }

  implicit class RichEntity(e:Entity) {
    def asDocument: RawJsonDocument = RawJsonDocument.create(idWrites.write(e.id), formats.writes(e).toString())
  }

}


@implicitNotFound(
  "No String serializer found for type ${T}. Try to implement an implicit IdWrites for this type."
)
trait IdWrites[T]{
  def write(id:T):String
}

object IdWrites{
  implicit val StringWrites: IdWrites[String] = (id: String) => id
}

/******************************* Repo instances **********************/

class FoodUnitRepo(val bucket:Observable[AsyncBucket])
                  (implicit val ec:ExecutionContext, val formats:Format[FoodUnit], val idWrites: IdWrites[FoodId]) extends CouchbaseRepository [FoodId, FoodUnit]{}

class RulesRepo(val bucket:Observable[AsyncBucket])
               (implicit val ec:ExecutionContext, val formats:Format[Rule], val idWrites: IdWrites[RuleId]) extends CouchbaseRepository [RuleId, Rule]{}
