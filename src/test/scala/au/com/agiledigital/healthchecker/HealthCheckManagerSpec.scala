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

import java.time.Duration

import akka.actor.ActorSystem
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext

/**
 * Contains unit tests for the [[HealthCheckManager]].
 */
class HealthCheckManagerSpec extends Specification {

  "Fetching health" should {
    "return last values from each checker" in {

      // Given a health check manager that contains a checker with a last value.
      val checker = new HealthChecker {
        override def checkAndUpdate()(implicit ec: ExecutionContext): Unit = ()

        override def lastValue: Option[HealthCheckResult] = Some(HealthCheckResult(
          HealthCheckStatus.Ok,
          this,
          "name",
          Some(10),
          "message",
          None,
          None,
          None
        ))

        override def period: Duration = Duration.ofMillis(1000)

        override def name: String = "test checker"

        override def isCritical: Boolean = false
      }

      val manager = new HealthCheckManager(Seq(checker), ActorSystem("test"))

      // When the health is fetched.
      val actual = manager.health()

      // Then it should contain the value returned by the checker.
      actual must contain(beLike[HealthCheckResult]{
        case result => result.value must beSome(10)
      })
    }
  }

}
