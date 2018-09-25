package com.sample.project.routes

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, PathMatcher1, Route}
import com.sample.project.domain.{ValidationSet, ValidationSetId}
import com.sample.project.repo.ValidationSetRepo
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

trait ValidationSetRoute extends PlayJsonSupport {

  def validationSetRoute(validationSetRepo: ValidationSetRepo): Route = {
    pathPrefix("validations") {
      pathEndOrSingleSlash {
        (post & entity(as[ValidationSet])) { validations ⇒
          onSuccess(validationSetRepo.get(validations.id)) {
            case Some(_) ⇒ complete(Conflict)
            case None ⇒ onSuccess(validationSetRepo.insert(validations)) { _ ⇒ complete(Created) }
          }
        }
      } ~
      pathPrefix(ValidationSetIdMatcher) { id ⇒
        get {
          onSuccess(validationSetRepo.get(id)){ complete(_) }
        } ~
        delete {
          onSuccess(validationSetRepo.delete(id)){_ ⇒ complete(OK)}
        }
      }
    }
  }

  val ValidationSetIdMatcher:PathMatcher1[ValidationSetId] = PathMatcher(Segment).map(s ⇒ ValidationSetId(s))
}
