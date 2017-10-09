package au.com.agiledigital.healthchecker.checkers

import au.com.agiledigital.healthchecker._

import play.api.Configuration

import au.com.agiledigital.healthchecker.HealthCheckOutcome._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Creates instances of the [[VersionHealthChecker]].
  */
class VersionHealthCheckerFactory extends HealthCheckerFactory {
  override def createHealthChecker(checkerConfiguration: Configuration): VersionHealthChecker =
    new VersionHealthChecker(checkerConfiguration)
}

/**
  * Health checker that displays the application build and version information.
  * as set in application.conf
  *
  * "environment" and "release" are presently used
  *
  * @param configuration the configuration for this checker. environment and build.release strings are extracted
  */
class VersionHealthChecker(override val configuration: Configuration)
    extends BaseHealthChecker with ConfigurationBasedHealthChecker {

  private val environment = configuration.getOptional[String]("name").getOrElse("Environment Unknown")
  private val release = configuration.getOptional[String]("release").getOrElse("Release Unknown")

  override val name = "Version Health Checker"

  override def doCheck()(implicit ec: ExecutionContext): Future[HealthCheckOutcome] =
    Future.successful(ok(
      Some(name),
      s"[$release] in [$environment]",
    ))
}
