import sbt._
import sbt.Keys._

object BuildSettings {
  val buildVersion = "1.0"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "au.com.agiledigital",
    version := buildVersion,
    scalaVersion := "2.11.1",
    crossScalaVersions := Seq("2.10", "2.11"),
    crossVersion := CrossVersion.binary,
    shellPrompt := ShellPrompt.buildShellPrompt)
}

// Shell prompt which show the current project,
// git branch and build version
object ShellPrompt {

  object devnull extends ProcessLogger {
    def info(s: => String) {}

    def error(s: => String) {}

    def buffer[T](f: => T): T = f
  }

  def currBranch = (
    ("git status -sb" lines_! devnull headOption)
    getOrElse "-" stripPrefix "## ")

  val buildShellPrompt = {
    (state: State) =>
      {
        val currProject = Project.extract(state).currentProject.id
        "%s:%s:%s> ".format(
          currProject, currBranch, BuildSettings.buildVersion)
      }
  }
}

object HealthCheckerBuild extends Build {
  import BuildSettings._

  lazy val healthchecker = Project(
    "Play2-Health-Checker",
    file("."),
    settings = buildSettings ++ Seq(
      publishMavenStyle := true,
      // To publish, put these credentials in ~/.m2/credentials
      //credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.agiledigital.com.au", "****", "****"),
      credentials += Credentials(Path.userHome / ".m2" / ".credentials"),
      publishTo := {
          val nexus = "http://nexus.agiledigital.com.au/nexus/"
          if (version.value.trim.endsWith("SNAPSHOT")) {
              Some("snapshots" at nexus + "content/repositories/snapshots")
          } else {
              Some("releases"  at nexus + "content/repositories/releases")
          }
      },
      resolvers := Seq(
        "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/maven-releases/"),
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-jdbc" % "2.3.3" cross CrossVersion.binary exclude("org.scala-stm", "scala-stm_2.10.0"),
        "com.typesafe.play" %% "play" % "2.3.3" cross CrossVersion.binary exclude("org.scala-stm", "scala-stm_2.10.0"))))
}
