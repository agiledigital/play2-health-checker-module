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

import akka.actor.Cancellable

import play.api.{Configuration, Logger}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

/**
  * Defines the interface of a regular health check action for any particular component of the system.
  */
trait HealthChecker {

  /**
    * The result of the last check.
    */
  def lastValue: Option[HealthCheckResult]

  /**
    * Checks the health of the component in the system. Updates the lastValue with the health.
    */
  def checkAndUpdate()(implicit ec: ExecutionContext): Unit

  /**
    * Name of the health check.
    */
  def name: String

  /**
    * Period of checks.
    */
  def period: Duration

  /**
    * Whether the functionality checked is critical to the function of the system.
    */
  def isCritical: Boolean
}

/**
  * Wraps a health checker and associated cancellable.
  */
case class CancellableHealthChecker(healthChecker: HealthChecker, cancellable: Cancellable) extends HealthChecker {

  override def lastValue: Option[HealthCheckResult] = healthChecker.lastValue

  override def checkAndUpdate()(implicit ec: ExecutionContext): Unit = healthChecker.checkAndUpdate()

  override def name: String = healthChecker.name

  override def period: Duration = healthChecker.period

  override def isCritical: Boolean = healthChecker.isCritical

  def cancel(): Boolean = cancellable.cancel()
}

/**
  * Partially implements the HealthChecker, using the supplied configuration to determine the frequency and criticality.
  */
trait ConfigurationBasedHealthChecker extends HealthChecker {

  private val DEFAULT_PERIOD: Duration = Duration.ofSeconds(5)

  def configuration: Configuration

  override lazy val period: Duration = configuration.getOptional[Long]("frequency").map(Duration.ofMillis).getOrElse(DEFAULT_PERIOD)

  override lazy val isCritical: Boolean = configuration.getOptional[Boolean]("critical").getOrElse(false)

}

/**
  * Base class for health checkers. Calculates the duration of the check and updates the last value.
  */
trait BaseHealthChecker extends HealthChecker {

  @volatile var lastValue: Option[HealthCheckResult] = None

  def checkAndUpdate()(implicit ec: ExecutionContext): Unit = {
    Logger.trace(s"Updating health checker [$name]...")

    val startTime = System.currentTimeMillis()
    val date      = Some(LocalDateTime.now())

    val triedOutcomeFut = Try {
      doCheck
    } recover {
      case NonFatal(e) =>
        Future.successful(HealthCheckOutcome(HealthCheckStatus.Error, None, Some(s"Failed executing health check [$name] - [$e]."), Some(e)))
    }

    triedOutcomeFut foreach { outcomeFut =>
      outcomeFut map { outcome =>
        val endTime = System.currentTimeMillis()
        HealthCheckResult(
          status = outcome.status,
          checker = this,
          name = name,
          value = outcome.value,
          message = outcome.message.getOrElse(""),
          throwable = outcome.throwable,
          lastChecked = date,
          lastDuration = Some(Duration.ofMillis(endTime - startTime))
        )
      } recover {
        case NonFatal(e) =>
          val endTime = System.currentTimeMillis()
          HealthCheckResult(
            status = HealthCheckStatus.Error,
            checker = this,
            name = name,
            value = None,
            message = s"Failed executing health check [$name] - [$e].",
            throwable = Some(e),
            lastChecked = date,
            lastDuration = Some(Duration.ofMillis(endTime - startTime))
          )
      } foreach { result =>
        lastValue = Some(result)
        result.status match {
          case HealthCheckStatus.Error   => Logger.error(s"Health check [$name] failed [$result].")
          case HealthCheckStatus.Warning => Logger.warn(s"Health check [$name] warning [$result].")
          case _                         => Logger.trace(s"Health check [$name] complete [$result].")
        }
      }
    }
  }

  def doCheck()(implicit ec: ExecutionContext): Future[HealthCheckOutcome]
}

/**
  * Describes the outcome of a [[BaseHealthChecker]] performing the check.
  */
final case class HealthCheckOutcome(status: HealthCheckStatus, value: Option[Any] = None, message: Option[String] = None, throwable: Option[Throwable] = None)

object HealthCheckOutcome {
  def ok(value: Option[Any], description: String) = HealthCheckOutcome(
    HealthCheckStatus.Ok,
    value,
    Some(description),
    None
  )

  def warning(value: Option[Any], description: String) = HealthCheckOutcome(
    HealthCheckStatus.Warning,
    value,
    Some(description),
    None
  )

  def error(value: Option[Any], description: String) = HealthCheckOutcome(
    HealthCheckStatus.Error,
    value,
    Some(description),
    None
  )

  def error(t: Throwable, description: String) = HealthCheckOutcome(
    HealthCheckStatus.Error,
    None,
    Some(description),
    Some(t)
  )

  def error(description: String) = HealthCheckOutcome(
    HealthCheckStatus.Error,
    None,
    Some(description),
    None
  )
}
