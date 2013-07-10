package au.com.agiledigital.healthchecker

import play.api.Logger
import play.api._
import play.libs.Akka
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._

class HealthCheckManager(private val healthCheckers: Traversable[HealthChecker]) {

  private val cancellableHealthCheckers = healthCheckers.map(cancellableChecker(_))

  def checkHealth(): Seq[HealthCheckResult] = this.cancellableHealthCheckers.map(healthChecker => healthChecker.lastValue match {
    case Some(status) => status
    case _ => HealthCheckResult(HealthCheckStatus.Unknown, healthChecker, healthChecker.description, None, "-", None, None, None)
  }).toSeq.sortBy(result => result.status)

  def overallStatus(healthCheckResults: Seq[HealthCheckResult]): HealthCheckStatus.Value = {
    healthCheckResults.find(h => h.status == HealthCheckStatus.Error && h.checker.isCritical) match {
      case Some(_) => HealthCheckStatus.Error
      case _ => HealthCheckStatus.Ok
    }
  }

  def stop(): Unit = this.cancellableHealthCheckers.foreach(checker => {
    Logger.debug("Stopping checker [%s]." format checker)
    checker.cancel()
  })

  /**
   * Wraps a [[HealthChecker]] with a [[CancellableHealthChecker]]. This allows
   * a checker to be cancelled at a later time.
   *
   * @return [[CancellableHealthChecker]]
   */
  private def cancellableChecker(c: HealthChecker): CancellableHealthChecker = {
    val cancellable = Akka.system.scheduler.schedule(new FiniteDuration(1, TimeUnit.SECONDS), new FiniteDuration(c.frequency, TimeUnit.MILLISECONDS)) {
      c.checkAndUpdate
    }
    CancellableHealthChecker(c, cancellable)
  }
}