package au.com.agiledigital.healthchecker

import play.api.Configuration
import play.api.Logger

/**
 * Checks that the total amount of free memory in the JVM is above the configured warning and error thresholds.
 * Total free memory is the sum of the free memory and unallocated memory.
 *
 * <pre>
 * freemem {
 *    provider = "hc.FreeMemoryHealthChecker"
 *    warning = 268435456
 *    error = 67108864    // Error at 64MB freemem.
 *    frequency = 1000
 * }
 * </pre>
 */
class FreeMemoryHealthChecker(config: Configuration, frequency: Long, isCritical: Boolean) extends BaseHealthChecker("Free Memory", config, frequency, isCritical) {

  val warningThreshold = config.getMilliseconds("warning")
  val errorThreshold = config.getMilliseconds("error")

  def doCheck: HealthCheckResult = {
    val runtime = Runtime.getRuntime()

    val maxMemory = runtime.maxMemory()
    val allocatedMemory = runtime.totalMemory()
    val freeMemory = runtime.freeMemory()

    val totalFreeMemory = (freeMemory + (maxMemory - allocatedMemory))

    if (errorThreshold.isDefined && totalFreeMemory < errorThreshold.get) {
      HealthCheckResult(
        HealthCheckStatus.Error,
        this,
        description,
        Some(totalFreeMemory),
        "Free memory [" + totalFreeMemory + "] less than error threshold [" + errorThreshold.get + "]",
        None,
        None,
        None)
    } else if (warningThreshold.isDefined && totalFreeMemory < warningThreshold.get) {
      HealthCheckResult(
        HealthCheckStatus.Warning,
        this,
        description,
        Some(totalFreeMemory),
        "Free memory [" + totalFreeMemory + "] less than warning threshold [" + errorThreshold.get + "]",
        None,
        None,
        None)
    } else {
      HealthCheckResult(
        HealthCheckStatus.Ok,
        this,
        description,
        Some(totalFreeMemory),
        "Free memory [" + totalFreeMemory + "] ok",
        None,
        None,
        None)
    }
  }

}