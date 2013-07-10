package au.com.agiledigital.healthchecker

import play.api.Configuration
import play.api.Logger
import play.libs.Akka
import java.util.Date
import akka.actor.Cancellable
import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * Define the interface of a regular health check action for any particular component of the system.
 */
trait HealthChecker {
  /**
   * The result of the last check.
   */
  def lastValue: Option[HealthCheckResult]
  /**
   * Checks the health of the component in the system. Updates the lastValue with the health.
   */
  def checkAndUpdate

  /**
   * Description of the health check.
   */
  def description: String

  /**
   * Frequency (in milliseconds) that the health check should be performed.
   */
  def frequency: Long

  /**
   * Whether the functionality checks is critical to the function of the system.
   */
  def isCritical: Boolean
}

/**
 * Wraps a health checker and associated Akka actor and provides a way to cancel the actor.
 */
case class CancellableHealthChecker(healthChecker: HealthChecker, cancellable: Cancellable) extends HealthChecker {

  override def lastValue = healthChecker.lastValue

  override def checkAndUpdate = healthChecker.checkAndUpdate

  override def description = healthChecker.description

  override def frequency = healthChecker.frequency

  override def isCritical = healthChecker.isCritical

  def cancel(): Unit = cancellable.cancel()

}

/**
 * Base class for health checkers. Calculates the duration of the check and updates the last value.
 */
abstract class BaseHealthChecker(val description: String, val config: Configuration, val frequency: Long, val isCritical: Boolean) extends HealthChecker {

  var lastValue: Option[HealthCheckResult] = None

  def checkAndUpdate = {
    Logger.trace("Updating health checker [" + frequency + "] [" + description + "].")

    val startTime = System.currentTimeMillis()
    val date = Some(new DateTime)

    val newResult = doCheck

    val endTime = System.currentTimeMillis()

    lastValue = Some(newResult.copy(lastChecked = date, lastDuration = Some(new Duration(endTime - startTime))))

    newResult.status match {
      case HealthCheckStatus.Error => Logger.error("Health check failed [" + newResult + "].")
      case HealthCheckStatus.Warning => Logger.warn("Health check warning [" + newResult + "].")
      case _ => Logger.trace("Health check complete [" + newResult + "].")
    }
  }

  def doCheck(): HealthCheckResult

}