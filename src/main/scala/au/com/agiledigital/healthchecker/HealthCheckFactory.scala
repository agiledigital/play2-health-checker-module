package au.com.agiledigital.healthchecker

import play.api.{Application, Configuration, Logger}

import scala.Option.option2Iterable
import scala.util.control.NonFatal

/**
 * Creates health checkers.
 */
object HealthCheckFactory {

  private val DEFAULT_FREQUENCY = 5000l // 5 seconds

  /**
   * Creates configured checkers and schedules them.
   *
   * Each subkey in the configuration is expected to contain the configuration for a [[HealthChecker]].
   *
   * @return the created and configured health checkers.
   */
  def processConfiguration(configuration: Configuration, application: Application): Iterable[HealthChecker] = {
    for (
      key <- configuration.subKeys;
      config <- configuration.getConfig(key);
      checker <- initChecker(key, config, application)
    ) yield checker
  }

  /**
   * Instantiates the checker and initialises it using the supplied configuration.
   *
   * If the checker can not be initialised, a checker will be returned that will
   * publish the details of the configuration failure.
   *
   * @pre config contains a key "provider" that resolves to the class name of a
   *      [[HealthChecker]] implementation.
   */
  private def initChecker(name: String, config: Configuration, application: Application): Option[HealthChecker] = {
    config.getString("provider") match {
      case Some(clazzName) => {
        createChecker(clazzName, name, config, application)
      }
      case _ => {
        val message = "No provider name specified in [%s]." format config
        val error = config.reportError("provider", message, None)
        Logger.error(message, error)
        Some(new BadConfigurationHealthChecker(name, message, Some(error)))
      }
    }
  }

  /**
   * Creates a [[HealthChecker]] of the type specified in clazzName.
   *
   * If the checker can not be instantiated, a checker will be returned that
   * will publish the details of the failure.
   */
  private def createChecker(clazzName: String, keyName: String, config: Configuration, application: Application): Option[HealthChecker] = {
    try {
      val frequency = config.getMilliseconds("frequency").getOrElse(DEFAULT_FREQUENCY)

      val isCritical = config.getBoolean("critical").getOrElse(true)

      val providerClazz: Class[_] = Class.forName(clazzName, true, application.classloader)

      val constructor = providerClazz.getConstructors()(0)

      val provider = constructor.newInstance(config, frequency: java.lang.Long, isCritical: java.lang.Boolean).asInstanceOf[HealthChecker]

      Some(provider)
    }
    catch {
      case NonFatal(e) => {
        val message = "No valid constructor found for [%s] in [%s]." format (clazzName, config)
        val error = config.reportError("provider", message, Some(e))
        Logger.error(message, error)
        Some(new BadConfigurationHealthChecker(keyName, message, Some(error)))
      }
    }
  }

}
