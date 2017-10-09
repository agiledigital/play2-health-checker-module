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

import javax.inject.{Inject, Provider, Singleton}

import akka.actor.ActorSystem

import au.com.agiledigital.healthchecker.checkers.BadConfigurationHealthChecker

import play.api._
import play.api.inject.{ApplicationLifecycle, Binding, Injector, Module}

import scala.concurrent.Future

/**
  * Provides the health checkers that have been configured.
  */
class HealthCheckerModule() extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(bind[HealthCheckManager].toProvider[HealthCheckManagerProvider].eagerly())

}

/**
  * Creates and destroys the HealthCheckManager.
  *
  * @param actorSystem the default actor system.
  * @param environment the injection environment.
  * @param configuration the application configuration.
  * @param lifecycle the application lifecycle.
  */
@Singleton
class HealthCheckManagerProvider @Inject()(actorSystem: ActorSystem, environment: Environment, configuration: Configuration, lifecycle: ApplicationLifecycle, injector: Injector)
    extends Provider[HealthCheckManager] {

  /** Key to the health check configuration. */
  private val healthCheckKey: String = "healthcheck"

  override def get(): HealthCheckManager = {
    // Initialise the health checks
    val healthCheckers = configuration.getOptional[Configuration](healthCheckKey) match {
      case Some(healthCheckConfiguration) =>
        Logger.info(s"Health check subsystem loading from configuration [$healthCheckConfiguration].")
        HealthCheckFactory.processConfiguration(healthCheckConfiguration, environment, injector)
      case None =>
        val message = s"Error loading health check configuration. No configuration defined at [$healthCheckKey]."
        val error   = configuration.reportError(healthCheckKey, s"No health checker configuration defined at [$healthCheckKey].", None)
        Logger.error(message, error)
        List(new BadConfigurationHealthChecker("Configuration Error", message, Some(error)))
    }

    val manager = new HealthCheckManager(healthCheckers, actorSystem)

    lifecycle.addStopHook(() => Future.successful(manager.stop()))

    manager
  }
}
