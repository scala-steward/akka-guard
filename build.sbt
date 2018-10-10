val commonSettings = Seq(
  sonatypeProfileName := "com.chatwork",
  organization := "com.chatwork",
  scalaVersion := "2.12.7",
  crossScalaVersions := Seq("2.11.11", "2.12.7"),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-encoding",
    "UTF-8",
    "-language:_"
  ) ++ {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2L, scalaMajor)) if scalaMajor == 12 =>
        Seq.empty
      case Some((2L, scalaMajor)) if scalaMajor <= 11 =>
        Seq(
          "-Yinline-warnings"
        )
    }
  },
  resolvers ++= Seq(
    "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"
  ),
  libraryDependencies ++= Seq(
    ScalaTest.v3_0_5      % Test,
    ScalaCheck.scalaCheck % Test,
    Akka.testKit          % Test,
    Cats.v1_4_0,
    Enumeratum.latest,
    Scala.java8Compat,
    Circe.core,
    Circe.generic,
    Circe.parser,
    Akka.actor
  ),
  updateOptions := updateOptions.value.withCachedResolution(true),
  parallelExecution in Test := false,
  javaOptions in (Test, run) ++= Seq("-Xms4g", "-Xmx4g", "-Xss10M", "-XX:+CMSClassUnloadingEnabled"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  pomExtra := {
    <url>https://github.com/chatwork/akka-guard</url>
    <licenses>
      <license>
        <name>The MIT License</name>
        <url>http://opensource.org/licenses/MIT</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:chatwork/akka-guard.git</url>
      <connection>scm:git:github.com/chatwork/akka-guard</connection>
      <developerConnection>scm:git:git@github.com:chatwork/akka-guard.git</developerConnection>
    </scm>
    <developers>
      <developer>
        <id>yoshiyoshifujii</id>
        <name>Yoshitaka Fujii</name>
      </developer>
    </developers>
  },
  publishTo in ThisBuild := sonatypePublishTo.value,
  credentials := {
    val ivyCredentials = (baseDirectory in LocalRootProject).value / ".credentials"
    Credentials(ivyCredentials) :: Nil
  }
)

lazy val `akka-guard-core` = (project in file("akka-guard-core"))
  .settings(commonSettings: _*)
  .settings(
    name := "akka-guard-core"
  )

lazy val `akka-guard-http` = (project in file("akka-guard-http"))
  .settings(commonSettings: _*)
  .settings(
    name := "akka-guard-http",
    libraryDependencies ++= Seq(
      AkkaHttp.testKit % Test,
      AkkaHttp.http
    )
  )
  .dependsOn(`akka-guard-core`)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "akka-guard"
  )
  .aggregate(`akka-guard-core`, `akka-guard-http`)