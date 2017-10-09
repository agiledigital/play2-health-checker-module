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

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem

import play.api.Logger

import scala.concurrent.duration.FiniteDuration

/**
  * Manages the health checkers. Provides access to the results of the checks and an overall health.
  *
  * Schedules checkers to run on their configured schedule.
  *
  * Wraps the supplied health checkers in cancellable wrappers.
  *
  * @param healthCheckers the health checkers to manage.
  * @param actorSystem the actor system to schedule the checkers within.
  */
class HealthCheckManager(healthCheckers: Traversable[HealthChecker], actorSystem: ActorSystem) {

  private val cancellableHealthCheckers = healthCheckers map {
    cancellableChecker
  }

  /**
    * Returns the last result of each health checker.
    * @return the health check results.
    */
  def health(): Seq[HealthCheckResult] =
    this.cancellableHealthCheckers
      .map(healthChecker =>
        healthChecker.lastValue match {
          case Some(status) => status
          case _            => HealthCheckResult(HealthCheckStatus.Unknown, healthChecker, healthChecker.name, None, "-", None, None, None)
      })
      .toSeq
      .sortBy(result => result.checker.name)

  /**
    * Calculates the overall health of the system, taking the criticality of each checker into account.
    * @param healthCheckResults the health check results to process.
    * @return the overall health of the system.
    */
  def overallStatus(healthCheckResults: Seq[HealthCheckResult]): HealthCheckStatus =
    healthCheckResults.find(h => h.status == HealthCheckStatus.Error && h.checker.isCritical) match {
      case Some(_) => HealthCheckStatus.Error
      case _       => HealthCheckStatus.Ok
    }

  /**
    * Stops the managed health checkers.
    */
  def stop(): Unit =
    this.cancellableHealthCheckers.foreach(checker => {
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
    import actorSystem.dispatcher
    val cancellable = actorSystem.scheduler.schedule(new FiniteDuration(1, TimeUnit.SECONDS), new FiniteDuration(c.period.toMillis, TimeUnit.MILLISECONDS)) {
      c.checkAndUpdate()
    }
    CancellableHealthChecker(c, cancellable)
  }
}
