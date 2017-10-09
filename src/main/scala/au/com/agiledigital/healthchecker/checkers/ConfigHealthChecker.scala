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

import javax.inject.Inject

import com.typesafe.config.ConfigObject

import au.com.agiledigital.healthchecker.HealthCheckerDSL._
import au.com.agiledigital.healthchecker.{BaseHealthChecker, ConfigurationBasedHealthChecker, HealthCheckOutcome, HealthChecker, HealthCheckerFactory}
import au.com.agiledigital.healthchecker.HealthCheckOutcome._

import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

class ConfigHealthCheckerFactory @Inject()(applicationConfiguration: Configuration) extends HealthCheckerFactory {
  override def createHealthChecker(checkerConfiguration: Configuration): HealthChecker =
    new ConfigHealthChecker(checkerConfiguration, applicationConfiguration)
}

/**
  * Displays a subsection of the application's configuration.
  *
  * Expected configuration:
  *
  * kamon_statsd {
  *   provider = au.com.agiledigital.healthchecker.ConfigHealthCheckerFactory
  *   frequency = 10000000
  *   configuration_path = "kamon.statsd"
  * }
  *
  * @param configuration the configuration of the checker.
  * @param applicationConfiguration the configuration of the application.
  */
class ConfigHealthChecker(override val configuration: Configuration, applicationConfiguration: Configuration) extends BaseHealthChecker with ConfigurationBasedHealthChecker with MonadicHealthChecks {

  val name = "Configuration Checker"

  override def doCheck()(implicit ec: ExecutionContext): Future[HealthCheckOutcome] =
    for {
      path                 <- configuration.getOptional[String]("configuration_path")  ?| error("Path to configuration to check must be set.")
      configurationToCheck <- applicationConfiguration.getOptional[ConfigObject](path) ?| error(s"Configuration for [$path] is not set.")
    } yield ok(Some(configurationToCheck), s"Configuration for [$path] is set to [$configurationToCheck].")
}
