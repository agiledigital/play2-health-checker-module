package au.com.agiledigital.healthchecker

import play.api.Configuration
import java.util.Date
import org.joda.time.DateTime

/**
 * Reports the uptime of the system. Always Ok.
 * <p/>
 * Example:
 * <pre>
 * uptime {
 *    provider = "hc.UptimeHealthChecker"
 *    frequency = 10000
 * }
 * </pre>
 */
class UptimeHealthChecker(config: Configuration, frequency: Long, isCritical: Boolean) extends BaseHealthChecker("Uptime", config, frequency, isCritical) {
  val createTime = System.currentTimeMillis()

  /**
   * Execute the health check reporting uptime, with string shown using Joda time formatting.
   */
  def doCheck: HealthCheckResult = {
    val uptime = System.currentTimeMillis() - createTime

    HealthCheckResult(
        HealthCheckStatus.Ok,
        this,
        description,
        Some(uptime),
        "Since [" + new DateTime(createTime) + "].",
        None,
        None,
        None)
  }
}
