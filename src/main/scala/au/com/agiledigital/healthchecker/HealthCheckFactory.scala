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

import au.com.agiledigital.healthchecker.checkers.BadConfigurationHealthChecker

import play.api.inject.Injector
import play.api.{Configuration, Environment, Logger}

import scala.util.control.NonFatal

/**
  * Creates health checkers.
  */
object HealthCheckFactory {

  /**
    * Creates configured checkers and schedules them.
    *
    * Each subkey in the configuration is expected to contain the configuration for a [[HealthChecker]].
    *
    * @return the created and configured health checkers.
    */
  def processConfiguration(configuration: Configuration, environment: Environment, injector: Injector): Iterable[HealthChecker] =
    for {
      key    <- configuration.subKeys
      config <- configuration.getOptional[Configuration](key)
    } yield initChecker(key, config, environment, injector)

  /**
    * Instantiates the checker and initialises it using the supplied configuration.
    *
    * If the checker can not be initialised, a checker will be returned that will
    * publish the details of the configuration failure.
    *
    * @pre config contains a key "provider" that resolves to the class name of a
    *      [[HealthChecker]] implementation.
    */
  private def initChecker(name: String, config: Configuration, environment: Environment, injector: Injector): HealthChecker =
    config.getOptional[String]("provider") map { clazzName =>
      createChecker(clazzName, name, config, environment, injector)
    } getOrElse {
      val message = s"No provider name specified in [$config]."
      val error   = config.reportError("provider", message, None)
      Logger.error(message, error)
      new BadConfigurationHealthChecker(name, message, Some(error))
    }

  /**
    * Creates a [[HealthChecker]] of the type specified in clazzName.
    *
    * If the checker can not be instantiated, a checker will be returned that
    * will publish the details of the failure.
    */
  private def createChecker(clazzName: String, keyName: String, config: Configuration, environment: Environment, injector: Injector): HealthChecker =
    try {
      val factoryClazz: Class[HealthCheckerFactory] = Class.forName(clazzName, true, environment.classLoader).asInstanceOf[Class[HealthCheckerFactory]]

      val factory = injector.instanceOf(factoryClazz)

      factory.createHealthChecker(config)
    } catch {
      case NonFatal(e) =>
        val message = s"No valid constructor found for [$clazzName] in [$config]."
        val error   = config.reportError("provider", message, Some(e))
        Logger.error(message, error)
        new BadConfigurationHealthChecker(keyName, message, Some(error))
    }

}

/**
  * Trait that supports a mixture of automatic DI (for the construction of the factory) and explicit DI by the
  * manager (for the construction of the checker).
  */
trait HealthCheckerFactory {
  def createHealthChecker(checkerConfiguration: Configuration): HealthChecker
}
