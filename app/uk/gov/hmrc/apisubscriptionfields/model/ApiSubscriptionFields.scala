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

package uk.gov.hmrc.apisubscriptionfields.model

import java.util.UUID

case class ApiSubscriptionRequest(applicationId: UUID,
																	apiContext: String,
																	apiVersion: String,
                                  customFields: Map[String, String] = Map.empty)

case class ApiSubscription(
  id: String,
  fieldsId: UUID,
  customFields: Map[String, String]
)

object ApiSubscription {
  def create(request: ApiSubscriptionRequest) = {
    ApiSubscription(
      // TODO: use / instead of _
      id = s"${request.applicationId.toString}_${request.apiContext}_${request.apiVersion}",
      fieldsId = UUID.randomUUID(),
      customFields = request.customFields
    )
  }
}
