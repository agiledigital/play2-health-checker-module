package au.com.agiledigital.healthchecker

import java.util.Date
import org.joda.time.DateTime
import org.joda.time.Duration
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Outcome of a health check.
 */
case class HealthCheckResult(
  status: HealthCheckStatus.Value,
  checker: HealthChecker,
  name: String,
  value: Option[Any],
  message: String,
  throwable: Option[Throwable],
  lastChecked: Option[DateTime],
  lastDuration: Option[Duration])

object HealthCheckResult {
  implicit object ThrowableWrites extends Writes[Throwable] {
    def writes(t: Throwable): JsValue = {
      JsObject(Seq(
        "message" -> JsString(t.getMessage()),
        "stacktrace" -> JsArray(t.getStackTrace().map(e => JsString(e.toString)))
      )
        ++ {
          if (t.getCause() != null && !t.getCause().equals(t)) {
            Seq("cause" -> Json.toJson(t.getCause()))
          }
          else {
            None
          }
        })
    }
  }

  implicit object DurationWrites extends Writes[Duration] {
    def writes(d: Duration): JsValue = JsNumber(d.getMillis())
  }

  implicit object HealthCheckStatusWrites extends Writes[HealthCheckStatus.Value] {
    def writes(s: HealthCheckStatus.Value): JsValue = JsString(s.toString)
  }

  implicit object HealthCheckResultWrites extends Writes[HealthCheckResult] {
    def writes(r: HealthCheckResult): JsValue = JsObject(Seq(
      "name" -> JsString(r.name),
      "checker" -> JsObject(Seq(
    		  "isCritical" -> JsBoolean(r.checker.isCritical),
    		  "frequency" -> JsNumber(r.checker.frequency)
      )),
      "message" -> JsString(r.message),
      "lastDuration" -> Json.toJson(r.lastDuration),
      "lastChecked" -> Json.toJson(r.lastChecked),
      "status" -> JsString(r.status.toString),
      "throwable" -> Json.toJson(r.throwable)
    ))
  }
}

/**
 * Health statuses that can be produced by a check.
 */
object HealthCheckStatus extends Enumeration {
  type HealthCheckStatus = Value
  val Warning = Value("Warning")
  val Error = Value("Error")
  val Unknown = Value("Unknown")
  val Ok = Value("Ok")

}