play2-health-checker-module
===========================
[![Build Status](https://travis-ci.org/agiledigital/play2-health-checker-module.svg?branch=master)](https://travis-ci.org/agiledigital/play2-health-checker-module)

To install locally
------------------

+ Clone the repository.
+ Change directory into the cloned repository.
+ Start <code>sbt</code>.
+ Publish to local Ivy repository with <code>publishLocal</code>.

To use in an application
------------------------
First, edit <code>build.sbt</code> to add the dependency:
```scala
  libraryDependencies ++= Seq(
    ...
    "au.com.agiledigital" %% "play2-health-checker" % "4.0.0"
    ...
  )
```

Edit <code>conf/application.conf</code> to add the dependency:
```scala
play.modules.enabled += "au.com.agiledigital.healthchecker.HealthCheckerModule"
```

Finally, add a route to the health check controller into <conf/routes>:
```scala
# Wire in the health check controller
GET     /hc                         au.com.agiledigital.healthchecker.controllers.HealthCheckController.checkHealth(serverErrorOnFailure: Boolean ?= true)
```
