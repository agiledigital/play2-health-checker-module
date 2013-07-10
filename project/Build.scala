import sbt._
import sbt.Keys._

object BuildSettings {
  val buildVersion = "1.0-SNAPSHOT"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "au.com.agiledigital",
    version := buildVersion,
    scalaVersion := "2.10.0",
    crossScalaVersions := Seq("2.10.0"),
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
      resolvers := Seq(
        "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"),
      libraryDependencies ++= Seq(
        "play" %% "play" % "2.1.0" cross CrossVersion.binary)))
}