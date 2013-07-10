package au.com.agiledigital.healthchecker

class BadConfigurationHealthChecker(key: String, errorMessage: String, maybeError: Option[Throwable]) extends HealthChecker {

  val description = "Failed configuration"

  lazy val lastValue = Some(HealthCheckResult(
    HealthCheckStatus.Error,
    this,
    key,
    None,
    errorMessage,
    maybeError,
    None,
    None
  ))

  def checkAndUpdate() = {}

  val frequency = -1l

  val isCritical = false
}