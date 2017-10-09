/*
 * Copyright 2016 Agile Digital Engineering P/L
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

package au.com.agiledigital.healthchecker.controllers

import javax.inject.Inject

import au.com.agiledigital.healthchecker.{HealthCheckManager, HealthCheckResult, HealthCheckStatus}

import play.api.libs.json.{Json, Writes}
import play.api.mvc.{BaseController, ControllerComponents}

import scala.util.control.NonFatal

/**
  * Provides an Action for displaying the health of the system.
  */
class HealthCheckController @Inject()(val controllerComponents: ControllerComponents, healthCheckerManager: HealthCheckManager) extends BaseController {

  /**
    * Renders the health check page. Will return a 500 if any checks are in Error.
    */
  def checkHealth(serverErrorOnFailure: Boolean) = Action {

    try {
      val healthCheckResults = healthCheckerManager.health()

      // Determine overall status.
      val overallStatus = healthCheckerManager.overallStatus(healthCheckResults)

      val result = Json.toJson(CheckHealthApiResponse(overallStatus, healthCheckResults))

      // Return results and status as JSON.
      overallStatus match {
        case HealthCheckStatus.Error if serverErrorOnFailure => InternalServerError(result)
        case _                                               => Ok(result)
      }
    } catch {
      case NonFatal(e) => throw new Exception("Failed to check system health.", e)
    }
  }
}

/**
  * Response sent when the health of the system is requested.
  * @param overallStatus overall status of the system.
  * @param healthCheckResults individual statuses of the components in the system.
  */
final case class CheckHealthApiResponse(overallStatus: HealthCheckStatus, healthCheckResults: Seq[HealthCheckResult])

/**
  * Companion that contains the JSON formats and writes.
  */
object CheckHealthApiResponse {

  import au.com.agiledigital.healthchecker.HealthCheckResult._

  implicit val writes: Writes[CheckHealthApiResponse] = Json.writes[CheckHealthApiResponse]
}
