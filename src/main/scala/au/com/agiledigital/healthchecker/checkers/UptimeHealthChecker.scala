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

import org.joda.time.DateTime

import au.com.agiledigital.healthchecker.HealthCheckOutcome._
import au.com.agiledigital.healthchecker.{BaseHealthChecker, ConfigurationBasedHealthChecker, HealthCheckOutcome, HealthChecker, HealthCheckerFactory}

import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

/**
  * Creates instances of the [[UptimeHealthChecker]].
  */
class UptimeHealthCheckerFactory extends HealthCheckerFactory {
  override def createHealthChecker(checkerConfiguration: Configuration): HealthChecker =
    new UptimeHealthChecker(checkerConfiguration)
}

/**
  * Reports the uptime of the system. Always Ok.
  * <p/>
  * Example:
  * <pre>
  * uptime {
  *    provider = "au.com.agiledigital.healthchecker.UptimeHealthCheckerFactory"
  *    frequency = 10000
  * }
  * </pre>
  */
class UptimeHealthChecker(override val configuration: Configuration) extends BaseHealthChecker with ConfigurationBasedHealthChecker {

  val name = "Uptime"

  private val createTime: Long = System.currentTimeMillis()

  private val createDateTime = new DateTime(createTime)

  /**
    * Execute the health check reporting uptime, with string shown using Joda time formatting.
    */
  override def doCheck()(implicit ec: ExecutionContext): Future[HealthCheckOutcome] = {
    val uptime = System.currentTimeMillis() - createTime

    Future.successful(ok(Some(uptime), s"Since [$createDateTime]."))
  }
}
