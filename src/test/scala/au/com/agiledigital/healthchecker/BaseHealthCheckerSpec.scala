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

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import java.time.Duration

/**
 * Contains unit tests for the [[BaseHealthChecker]].
 */
class BaseHealthCheckerSpec(implicit ev: ExecutionEnv) extends Specification {

  "Base health checker" should {
    "handle a checker that throws an exception" in {
      // Given a health checker that throws an exception
      val exception = new RuntimeException("Expected exception")
      val checker = new BaseHealthChecker {

        override def doCheck()(implicit ec: ExecutionContext): Future[HealthCheckOutcome] =
          throw exception

        override def period: Duration = Duration.ofMillis(1)

        override def name: String = "Bad checker"

        override def isCritical: Boolean = false
      }

      // When the checker is checked.
      checker.checkAndUpdate()

      // Then no exception should have been thrown and the status should have been updated.
      eventually(50, 100 millis) {
        checker.lastValue must beSome[HealthCheckResult](
          beLike[HealthCheckResult] {
            case result: HealthCheckResult =>
              (result.status must_== HealthCheckStatus.Error) and
                (result.throwable must beSome(exception))
          }
        )
      }
    }
    "handle a checker that returns a failed future" in {
      // Given a health checker that returns a failed future.
      val exception = new RuntimeException("Expected exception")
      val checker = new BaseHealthChecker {

        override def doCheck()(implicit ec: ExecutionContext): Future[HealthCheckOutcome] =
          Future.failed(exception)

        override def period: Duration = Duration.ofMillis(1)

        override def name: String = "Bad checker"

        override def isCritical: Boolean = false
      }

      // When the checker is checked.
      checker.checkAndUpdate()

      eventually(50, 100 millis) {
        // Then no exception should have been thrown and the status should have been updated.
        checker.lastValue must beSome[HealthCheckResult](
          beLike[HealthCheckResult] {
            case result: HealthCheckResult =>
              (result.status must_== HealthCheckStatus.Error) and
                (result.throwable must beSome(exception))
          }
        )
      }
    }
    "handle a checker that returns a successful future" in {
      // Given a health checker that returns a failed future.
      val checker = new BaseHealthChecker {

        override def doCheck()(implicit ec: ExecutionContext): Future[HealthCheckOutcome] =
          Future.successful(
            HealthCheckOutcome(HealthCheckStatus.Ok, Some(10L), Some("message"), None)
          )

        override def period: Duration = Duration.ofMillis(1)

        override def name: String = "Bad checker"

        override def isCritical: Boolean = false
      }

      // When the checker is checked.
      checker.checkAndUpdate()

      // Then no exception should have been thrown and the status should have been updated.
      eventually(50, 100 millis) {
        checker.lastValue must beSome[HealthCheckResult](
          beLike[HealthCheckResult] {
            case result: HealthCheckResult =>
              (result.status must_== HealthCheckStatus.Ok) and
                (result.message must_== "message") and
                (result.value must beSome(10L)) and
                (result.throwable must beNone)
          }
        )
      }
    }
  }

}
