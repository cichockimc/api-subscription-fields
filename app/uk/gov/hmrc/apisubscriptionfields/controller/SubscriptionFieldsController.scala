/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apisubscriptionfields.controller

import java.util.UUID
import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc._
import uk.gov.hmrc.apisubscriptionfields.model.ErrorCode._
import uk.gov.hmrc.apisubscriptionfields.model._
import uk.gov.hmrc.apisubscriptionfields.service.SubscriptionFieldsService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubscriptionFieldsController @Inject()(service: SubscriptionFieldsService) extends CommonController {

  import JsonFormatters._

  private def notFoundResponse(message: String) = {
    NotFound(JsErrorResponse(NOT_FOUND_CODE, message))
  }

  private def notFoundMessage(rawClientId: String, rawApiContext: String, rawApiVersion: String): String = {
    s"Id ($rawClientId, $rawApiContext, $rawApiVersion) was not found"
  }

  private def notFoundMessage(rawFieldsId: UUID): String = {
    s"FieldsId (${rawFieldsId.toString}) was not found"
  }

  private def notFoundMessage(rawClientId: String): String = {
    s"ClientId ($rawClientId) was not found"
  }

  def getSubscriptionFields(rawClientId: String, rawApiContext: String, rawApiVersion: String): Action[AnyContent] = Action.async { implicit request =>
    Logger.debug(s"[getSubscriptionFields] clientId: $rawClientId apiContext: $rawApiContext apiVersion: $rawApiVersion")
    val eventualMaybeResponse = service.get(SubscriptionIdentifier(ClientId(rawClientId), ApiContext(rawApiContext), ApiVersion(rawApiVersion)))
    asActionResult(eventualMaybeResponse, notFoundMessage(rawClientId, rawApiContext, rawApiVersion))
  }

  def getSubscriptionFieldsByFieldsId(rawFieldsId: UUID): Action[AnyContent] = Action.async { implicit request =>
    Logger.debug(s"[getSubscriptionFieldsByFieldsId] fieldsId: $rawFieldsId")
    val eventualMaybeResponse = service.get(SubscriptionFieldsId(rawFieldsId))
    asActionResult(eventualMaybeResponse, notFoundMessage(rawFieldsId))
  }

  def getBulkSubscriptionFieldsByClientId(rawClientId: String): Action[AnyContent] = Action.async { implicit request =>
    Logger.debug(s"[getBulkSubscriptionFieldsByClientId] clientId: $rawClientId")
    val eventualMaybeResponse = service.get(ClientId(rawClientId))
    asBulkActionResult(eventualMaybeResponse, notFoundMessage(rawClientId))
  }

  private def asActionResult[T](eventualMaybeResponse: Future[Option[T]], notFoundMessage: String)(implicit writes: Writes[T]) = {
    eventualMaybeResponse map {
      case Some(payload) => Ok(Json.toJson(payload))
      case None => notFoundResponse(notFoundMessage)
    } recover recovery
  }

  private def asBulkActionResult(eventualMaybeResponse: Future[Option[BulkSubscriptionFieldsResponse]], notFoundMessage: String) = {
    eventualMaybeResponse map {
      case Some(subscriptionFields) => Ok(Json.toJson(subscriptionFields))
      case None => notFoundResponse(notFoundMessage)
    } recover recovery
  }

  def upsertSubscriptionFields(rawClientId: String, rawApiContext: String, rawApiVersion: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
      withJsonBody[SubscriptionFieldsRequest] { payload =>
        Logger.debug(s"[upsertSubscriptionFields] clientId: $rawClientId apiContext: $rawApiContext apiVersion: $rawApiVersion")
        service.upsert(SubscriptionIdentifier(ClientId(rawClientId), ApiContext(rawApiContext), ApiVersion(rawApiVersion)), payload.fields) map {
          case (response, true) => Created(Json.toJson(response))
          case (response, false) => Ok(Json.toJson(response))
        } recover {
          case e: Exception => BadRequest(e.getMessage)
        }
      } recover recovery
    }

  def deleteSubscriptionFields(rawClientId: String, rawApiContext: String, rawApiVersion: String): Action[AnyContent] = Action.async { implicit request =>
    val identifier = SubscriptionIdentifier(ClientId(rawClientId), ApiContext(rawApiContext), ApiVersion(rawApiVersion))
    Logger.debug(s"[deleteSubscriptionFields] clientId: $rawClientId apiContext: $rawApiContext apiVersion: $rawApiVersion")

    service.delete(identifier) map {
      case true => NoContent
      case false => notFoundResponse(notFoundMessage(rawClientId, rawApiContext, rawApiVersion))
    } recover recovery
  }

}