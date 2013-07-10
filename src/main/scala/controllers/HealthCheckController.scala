package controllers

import play.mvc.Controller
import play.api._
import play.api.Play.current
import play.api.mvc._
import play.api.mvc.Results._
import java.util.Date
import views.html
import au.com.agiledigital.healthchecker.HealthCheckerPlugin
import au.com.agiledigital.healthchecker.HealthCheckResult
import au.com.agiledigital.healthchecker.HealthCheckResult._
import au.com.agiledigital.healthchecker.HealthCheckStatus
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsArray

/**
 * Provides an Action for displaying the health of the system.
 */
object HealthCheckController extends Controller {

  /**
   * Renders the health check page. Will return a 500 if any checks are in Error.
   */
  def checkHealth = Action {
    // Replace missing statuses with Unknown statuses.
    val maybeHealthCheckerPlugin = Play.application.plugin[HealthCheckerPlugin]
    maybeHealthCheckerPlugin match {
      case Some(healthCheckerPlugin) => {

        val healthCheckResults = healthCheckerPlugin.healthCheckerManager.checkHealth

        // Determine overall status.
        val overallStatus = healthCheckerPlugin.healthCheckerManager.overallStatus(healthCheckResults)

        val result = JsObject(Seq(
          "status" -> Json.toJson(overallStatus),
          "results" -> JsArray(healthCheckResults.map(Json.toJson(_)))
        ))

        // Return results and status as JSON.
        overallStatus match {
          case HealthCheckStatus.Error => InternalServerError(result)
          case _ => Ok(result)
        }
      }
      case _ => InternalServerError("Health check plugin not loaded. Has it been added to \"conf/play.plugins\"?")
    }

  }

}