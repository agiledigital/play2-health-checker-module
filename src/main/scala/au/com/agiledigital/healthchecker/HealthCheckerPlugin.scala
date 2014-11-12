package au.com.agiledigital.healthchecker

import play.api._

class HealthCheckerPlugin(app: Application) extends Plugin {

  /**
   * The manager for system diagnostic health checkers that are configured to run at startup.
   */
  var healthCheckerManager: HealthCheckManager = new HealthCheckManager(List())

  override def onStart {
    Logger.info("HealthChecker starting...")

    // Initialise the health checks
    val maybeHealthCheckConfiguration = app.configuration.getConfig("healthcheck")
    maybeHealthCheckConfiguration match {
      case Some(healthCheckConfiguration) => {
        Logger.info("Health check subsystem loading from configuration [" + healthCheckConfiguration + "].")
        val healthCheckers = HealthCheckFactory.processConfiguration(healthCheckConfiguration, app)
        this.healthCheckerManager = new HealthCheckManager(healthCheckers)
      }
      case None => {
        val message = "Error loading health check configuration. No configuration defined at [healthcheck]."
        val error = app.configuration.reportError("healthcheck", "No health checker configuration defined at [healthcheck].", None)
        Logger.error(message, error)
        this.healthCheckerManager = new HealthCheckManager(List(new BadConfigurationHealthChecker("Configuration Error", message, Some(error))))
      }
    }

    Logger.info("HealthChecker started.")
  }

  override def onStop {
    Logger.info("HealthChecker stopping...")

    this.healthCheckerManager.stop()

    Logger.info("HealthChecker stopped.")
  }



}