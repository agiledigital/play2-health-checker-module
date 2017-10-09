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

import au.com.agiledigital.healthchecker.HealthCheckOutcome._
import au.com.agiledigital.healthchecker.{BaseHealthChecker, ConfigurationBasedHealthChecker, HealthCheckOutcome, HealthChecker, HealthCheckerFactory}

import play.api.{Configuration, Environment}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Creates instances of the [[ApplicationNameHealthChecker]].
  */
class ApplicationNameHealthCheckerFactory @Inject()(environment: Environment) extends HealthCheckerFactory {
  override def createHealthChecker(checkerConfiguration: Configuration): HealthChecker =
    new ApplicationNameHealthChecker(environment, checkerConfiguration)
}

/**
  * Health checker that displays the application name and the mode that the application is running in.
  *
  * @param environment the environment.
  * @param configuration the configuration for this checker.
  */
class ApplicationNameHealthChecker(environment: Environment, override val configuration: Configuration) extends BaseHealthChecker with ConfigurationBasedHealthChecker {

  override val name: String = configuration.getOptional[String]("name").getOrElse("No Name")

  override def doCheck()(implicit ec: ExecutionContext): Future[HealthCheckOutcome] =
    Future.successful(ok(Option(name), s"[$name] running in [${environment.mode}] mode"))
}
