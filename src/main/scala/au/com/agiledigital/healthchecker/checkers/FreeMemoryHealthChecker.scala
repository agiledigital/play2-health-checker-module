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

package au.com.agiledigital.healthchecker.checkers

import au.com.agiledigital.healthchecker.HealthCheckOutcome._
import au.com.agiledigital.healthchecker.{BaseHealthChecker, ConfigurationBasedHealthChecker, HealthCheckOutcome, HealthChecker, HealthCheckerFactory}

import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

/**
  * Creates instances of the [[FreeMemoryHealthChecker]].
  */
class FreeMemoryHealthCheckerFactory extends HealthCheckerFactory {
  override def createHealthChecker(checkerConfiguration: Configuration): HealthChecker =
    new FreeMemoryHealthChecker(checkerConfiguration)
}

/**
  * Checks that the total amount of free memory in the JVM is above the configured warning and error thresholds.
  * Total free memory is the sum of the free memory and unallocated memory.
  *
  * <pre>
  * freemem {
  *    provider = "hc.FreeMemoryHealthChecker"
  *    warning = 268435456
  *    error = 67108864    // Error at 64MB freemem.
  *    frequency = 1000
  * }
  * </pre>
  */
class FreeMemoryHealthChecker(override val configuration: Configuration) extends BaseHealthChecker with ConfigurationBasedHealthChecker {

  override val name = "Free Memory"

  private val maybeWarningThreshold = configuration.getOptional[Long]("warning")
  private val maybeErrorThreshold   = configuration.getOptional[Long]("error")

  override def doCheck()(implicit ec: ExecutionContext): Future[HealthCheckOutcome] = {
    val runtime = Runtime.getRuntime

    val maxMemory       = runtime.maxMemory()
    val allocatedMemory = runtime.totalMemory()
    val freeMemory      = runtime.freeMemory()

    val totalFreeMemory = freeMemory + (maxMemory - allocatedMemory)

    val outcome = (maybeErrorThreshold, maybeWarningThreshold) match {
      case (Some(errorThreshold), _) if totalFreeMemory < errorThreshold     => error(Some(totalFreeMemory), s"Free memory [$totalFreeMemory] less than error threshold [$errorThreshold].")
      case (_, Some(warningThreshold)) if totalFreeMemory < warningThreshold => warning(Some(totalFreeMemory), s"Free memory [$totalFreeMemory] less than warning threshold [$warningThreshold].")
      case _                                                                 => ok(Some(totalFreeMemory), s"Free memory [$totalFreeMemory] ok.")
    }

    Future.successful(outcome)
  }

}
