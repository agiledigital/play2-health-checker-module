package au.com.agiledigital.healthchecker.checkers

import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.DataTables
import org.specs2.mutable.Specification

import au.com.agiledigital.healthchecker.HealthCheckStatus

import play.api.Configuration

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
  * Tests for [[VersionHealthChecker]].
  */
class VersionHealthCheckerSpec(implicit ev: ExecutionEnv) extends Specification with DataTables {

  "VersionHealthChecker" in {
    // format: OFF
      "environment"       || "release"    || "expected message_matcher" |>
      None                !! None         !! contain("[Environment Unknown]") |
      None                !! None         !! contain("[Release Unknown]") |
      None                !! Some("V1")   !! contain("[Environment Unknown]") |
      None                !! Some("V1")   !! contain("[V1]") |
      Some("uat")         !! Some("V2")   !! contain("[V2]") |
      Some("uat")         !! Some("V2")   !! contain("[uat]") |
      Some("uat")         !! Some("V2")   !! not(contain("Unknown")) |
      Some("staging")     !! None         !! contain("[Release Unknown]") |
      Some("staging")     !! None         !! contain("[staging]") |> {
        // format: ON
        (environment, release, expectedMessage) =>
          {
            val configuration = Configuration.from(Map.empty[String, String] ++ release.map(r => "release" -> r) ++ environment.map(e => "name" -> e))

            val checker = new VersionHealthChecker(configuration)

            val result = Await.result(checker.doCheck(), 5 seconds)
            result.status must_== HealthCheckStatus.Ok
            result.message aka s"Result message [${result.message}] should incorporate [$environment] and [$release]." must beSome(expectedMessage)
          }
      }
  }
}
