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

package au.com.agiledigital.healthchecker

import java.time.{Duration, LocalDateTime}

import play.api.libs.json._

/**
  * Outcome of a health check.
  */
case class HealthCheckResult(status: HealthCheckStatus,
                             checker: HealthChecker,
                             name: String,
                             value: Option[Any],
                             message: String,
                             throwable: Option[Throwable] = None,
                             lastChecked: Option[LocalDateTime] = None,
                             lastDuration: Option[Duration] = None)

/**
  * Health statuses that can be produced by a check.
  */
sealed trait HealthCheckStatus
object HealthCheckStatus {
  case object Warning extends HealthCheckStatus
  case object Error   extends HealthCheckStatus
  case object Unknown extends HealthCheckStatus
  case object Ok      extends HealthCheckStatus
}

object HealthCheckResult {
  implicit object ThrowableWrites extends Writes[Throwable] {
    def writes(t: Throwable): JsValue =
      JsObject(
        Seq(
          "message"    -> JsString(t.getMessage),
          "stacktrace" -> JsArray(t.getStackTrace.map(e => JsString(e.toString)))
        )
          ++ {
            if (t.getCause != null && !t.getCause.equals(t)) {
              Seq("cause" -> Json.toJson(t.getCause))
            } else {
              None
            }
          })
  }

  implicit object DurationWrites extends Writes[Duration] {
    def writes(d: Duration): JsValue = JsNumber(d.toMillis)
  }

  implicit object HealthCheckStatusWrites extends Writes[HealthCheckStatus] {
    def writes(s: HealthCheckStatus): JsValue = JsString(s.toString)
  }

  implicit object HealthCheckResultWrites extends Writes[HealthCheckResult] {
    def writes(r: HealthCheckResult): JsValue =
      JsObject(
        Seq(
          "name" -> JsString(r.name),
          "checker" -> JsObject(
            Seq(
              "isCritical" -> JsBoolean(r.checker.isCritical),
              "frequency"  -> JsNumber(r.checker.period.toMillis)
            )),
          "message"      -> JsString(r.message),
          "lastDuration" -> Json.toJson(r.lastDuration),
          "lastChecked"  -> Json.toJson(r.lastChecked),
          "status"       -> JsString(r.status.toString),
          "throwable"    -> Json.toJson(r.throwable)
        ))
  }
}
