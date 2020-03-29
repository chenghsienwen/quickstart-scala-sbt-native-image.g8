ThisBuild / scalaVersion := "2.12.10"
ThisBuild / organization := "$organization$"
ThisBuild / maintainer := "$maintainer$"
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings")
ThisBuild / javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

// resolve from environment( jennkins pipeline )
val nexusRepository = "http://nexus.vpon.com/content/repositories/"
val UNKNOWN_SETTING: String = "unknown"
val branchName = sys.env.getOrElse("BRANCH_NAME", UNKNOWN_SETTING)
val buildNumber = sys.env.getOrElse("BUILD_NUMBER", "latest")
val nexusAccount = sys.env.getOrElse("NEXUS_ACCOUNT", "")
val nexusPassword = sys.env.getOrElse("NEXUS_PASSWORD", "")


import sbt.Resolver
enablePlugins(GitVersioning)
val gitHeadCode = SettingKey[String]("git-head-hash", "The commit hash code of HEAD")
gitHeadCode := git.gitHeadCommit.value
  .map { sha =>
      s"$"$"${sha.take(7)}"
  }
  .getOrElse("na")

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, GraalVMNativeImagePlugin)
  .settings(
    name := "$name$",
    version := s"$"$"${buildNumber}_$"$"${gitHeadCode.value}",

    // Options used by `native-image` when building native image.
    // https://www.graalvm.org/docs/reference-manual/native-image/
    graalVMNativeImageOptions ++= Seq(
        "--initialize-at-build-time", // Auto-packs dependent libs at build-time
        "--no-fallback", // Bakes-in run-time reflection (alternately: --auto-fallback, --force-fallback)
        "--no-server", // Won't be running `graalvm-native-image:packageBin` often, so one less thing to break
        "--static" // Disable for OSX (non-docker) builds - Forces statically-linked binary, compatible with libc (linux)
    ),

    // Concurrency:
    libraryDependencies += "io.monix" %% "monix" % "3.1.0",

    // Arg Parsing:
    libraryDependencies += "com.github.scopt" %% "scopt" % "4.0.0-RC2",

    // Logging:
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3", // See README.md for why we don't use log4j2

    // Testing:
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
  )


publishTo := {
    branchName match {
        case "master" => Some("VDAS releases repository" at nexusRepository + "releases")
        case "dev" => Some("VDAS releases repository" at nexusRepository + "snapshots")
        case UNKNOWN_SETTING =>
            println("due to lack of branchName environment variable, use local repository publish setting")
            Some(Resolver.file("file", new File(Path.userHome.absolutePath + "/.m2/repository")))
        case _ => Some("VDAS developments repository" at nexusRepository + "developments")
    }
}

credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.vpon.com", nexusAccount, nexusPassword)
publishMavenStyle := true
publishArtifact in (Compile, packageDoc) := false
publishArtifact in Test := false


addCommandAlias("validate", ";clean;coverage;test;coverageReport;coverageAggregate")
