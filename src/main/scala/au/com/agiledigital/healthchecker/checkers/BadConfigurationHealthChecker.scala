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

import java.time.Duration

import au.com.agiledigital.healthchecker.{HealthCheckResult, HealthCheckStatus, HealthChecker}

import scala.concurrent.ExecutionContext

class BadConfigurationHealthChecker(key: String, errorMessage: String, maybeError: Option[Throwable]) extends HealthChecker {

  override val name = "Failed configuration"

  lazy val lastValue = Some(
    HealthCheckResult(
      HealthCheckStatus.Error,
      this,
      key,
      None,
      errorMessage,
      maybeError,
      None,
      None
    ))

  override def checkAndUpdate()(implicit ec: ExecutionContext): Unit = {}

  override val period: Duration = Duration.ZERO

  override val isCritical: Boolean = false
}
