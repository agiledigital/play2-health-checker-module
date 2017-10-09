package au.com.agiledigital.healthchecker.controllers

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import au.com.agiledigital.healthchecker.{HealthCheckManager, HealthCheckResult, HealthCheckStatus}

import play.api.libs.json.{JsDefined, JsString}

import play.api.test.Helpers._
import play.api.test.FakeRequest

/**
  * Contains unit tests for the [[HealthCheckController]].
  */
class HealthCheckControllerSpec extends Specification with Mockito {

  "Checking the health of the system" should {
    "return an OK with the status" in new WithHealthCheckManager {

      // Given a health check manager that returns no results.
      healthCheckerManager.health() returns Nil
      // And an overall status of ok.
      healthCheckerManager.overallStatus(any[Seq[HealthCheckResult]]) returns HealthCheckStatus.Ok

      // When the health check status is fetched.
      val result = controller.checkHealth(serverErrorOnFailure = false)(FakeRequest())

      // Then a 200 should have been returned.
      status(result) must_=== OK

      // And is should contain the status.
      val json = contentAsJson(result)

      (json \ "overallStatus") must_=== JsDefined(JsString("Ok"))
    }
    "return an OK with the error status if the server error on failure flag is false" in new WithHealthCheckManager {
      // Given a health check manager that returns no results.
      healthCheckerManager.health() returns Nil
      // And an overall status of ERROR.
      healthCheckerManager.overallStatus(any[Seq[HealthCheckResult]]) returns HealthCheckStatus.Error

      // When the health check status is fetched with server error on failure set to false
      val result = controller.checkHealth(serverErrorOnFailure = false)(FakeRequest())

      // Then a 200 should have been returned.
      status(result) must_=== OK

      // And is should contain the status.
      val json = contentAsJson(result)

      (json \ "overallStatus") must_=== JsDefined(JsString("Error"))
    }
    "return an InternalServerError with the error status if the server error on failure flag is true" in new WithHealthCheckManager {
      // Given a health check manager that returns no results.
      healthCheckerManager.health() returns Nil
      // And an overall status of ERROR.
      healthCheckerManager.overallStatus(any[Seq[HealthCheckResult]]) returns HealthCheckStatus.Error

      // When the health check status is fetched with server error on failure set to true
      val result = controller.checkHealth(serverErrorOnFailure = true)(FakeRequest())

      // Then a 500 should have been returned.
      status(result) must_=== INTERNAL_SERVER_ERROR

      // And is should contain the status.
      val json = contentAsJson(result)

      (json \ "overallStatus") must_=== JsDefined(JsString("Error"))
    }
  }

  trait WithHealthCheckManager extends Scope {

    lazy val healthCheckerManager: HealthCheckManager = mock[HealthCheckManager]

    lazy val controller = new HealthCheckController(stubControllerComponents(), healthCheckerManager)
  }
}
