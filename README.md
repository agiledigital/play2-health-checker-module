play2-health-checker-module
===========================

To install locally
------------------

+ Clone the repository.
+ Change directory into the cloned repository.
+ Start <code>sbt</code>.
+ Publish to local Ivy repository with <code>publish-local</code>.
+ Note the location that it was published to. It should be something like <code>/Users/dd3/.ivy2/local/</code>.

To use in an application
------------------------
First, edit <code>project/Build.scala</code> to add the dependency:
```scala
  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    "au.com.agiledigital" % "play2-health-checker_2.10" % "1.0-SNAPSHOT"
  )
```

And the same file to add the local Ivy repository:
```scala
  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    resolvers += Resolver.url("Local Ivy", new URL("file:///Users/dd3/.ivy2/local/"))(Resolver.ivyStylePatterns)
  )
```
Replace /Users/dd3 with the lcoation that you noted when installing the plugin.

Edit <code>conf/play.plugins</code> to add the dependency:
```scala
500:au.com.agiledigital.healthchecker.HealthCheckerPlugin
```

Finally, add a route to the health check controller into <conf/routes>:
```scala
# Wire in the health check controller
GET     /hc                         controllers.HealthCheckController.checkHealth
```
