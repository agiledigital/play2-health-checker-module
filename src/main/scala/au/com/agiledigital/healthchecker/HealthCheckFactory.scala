package au.com.agiledigital.healthchecker

import scala.Option.option2Iterable
import scala.reflect.api.Mirror
import scala.reflect.runtime.universe.nme
import scala.reflect.runtime.currentMirror

import play.api.Configuration
import play.api.Logger

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
  def processConfiguration(configuration: Configuration): Iterable[HealthChecker] = {
    for (
      key <- configuration.subKeys;
      config <- configuration.getConfig(key);
      checker <- initChecker(key, config)
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
  private def initChecker(name: String, config: Configuration): Option[HealthChecker] = {
    config.getString("provider") match {
      case Some(clazzName) => {
        createChecker(clazzName, name, config)
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
  private def createChecker(clazzName: String, keyName: String, config: Configuration): Option[HealthChecker] = {
    try {
      val frequency = config.getMilliseconds("frequency").getOrElse(DEFAULT_FREQUENCY)

      val isCritical = config.getBoolean("critical").getOrElse(true)

      val m = scala.reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)
      val providerClazz = m.staticClass(clazzName)

      val classMirror = currentMirror.reflectClass(providerClazz)

      val providerConstructor = providerClazz.toType.declaration(nme.CONSTRUCTOR).asMethod

      val provider = classMirror.
        reflectConstructor(providerConstructor)(config, frequency: java.lang.Long, isCritical: java.lang.Boolean).asInstanceOf[HealthChecker]

      Some(provider)
    }
    catch {
      case t: Throwable => {
        val message = "No valid constructor found for [%s] in [%s]." format (clazzName, config)
        val error = config.reportError("provider", message, Some(t))
        Logger.error(message, error)
        Some(new BadConfigurationHealthChecker(keyName, message, Some(error)))
      }
    }
  }

}