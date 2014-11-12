package au.com.agiledigital.healthchecker

import play.api.Configuration
import play.api.db.DB
import play.api.Play.current

/**
 * Created by dxspasojevic on 25/09/2014.
 * Copyright (c) 2014 Agile Digital Pty Ltd. All rights reserved.
 */
class DatabaseHealthChecker(config: Configuration, frequency: Long, isCritical: Boolean) extends BaseHealthChecker("Database", config, frequency, isCritical) {

  val datasourceName = config.getString("datasource").get
  val statement = config.getString("statement").getOrElse("select 1;")

  def doCheck: HealthCheckResult = {

    try {
      val connection = DB.getConnection(datasourceName)
      try {
        val result = connection.createStatement.execute(statement)
        HealthCheckResult(
          HealthCheckStatus.Ok,
          this,
          description,
          Some(result),
          "Sent [" + statement + "] to relational database [" + connection.getMetaData().getURL() + "].",
          None, None, None)
      }
      catch {
        case e: Throwable => HealthCheckResult(
          HealthCheckStatus.Error,
          this,
          description,
          None,
          "Error sending [" + statement + "] to db [" + datasourceName + "]",
          Some(e), None, None)
      }
      finally {
        connection.close()
      }
    }
    catch {
      case e: Throwable => HealthCheckResult(
        HealthCheckStatus.Error,
        this,
        description,
        None,
        "Error getting connection for db [" + datasourceName + "]",
        Some(e), None, None)
    }
  }

}
