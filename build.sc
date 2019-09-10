import $ivy.`com.lihaoyi::mill-contrib-buildinfo:0.5.1-9-f9a999`
import $ivy.`com.lihaoyi::mill-contrib-bloop:0.5.1-9-f9a999`
import mill._, scalalib._, publish._
import contrib.BuildInfo

final object Globals {
  @inline final val version: String =
    os.proc("git", "describe", "--tags").call().out.string.strip
}

trait Github4sModule extends ScalaModule {

  @inline final def publishVersion = Globals.version

  @inline final def scalaVersion = "2.13.0"

  @inline final def scalacOptions = Seq(
    // "-P:acyclic:force",
    "-target:jvm-1.8",
    "-encoding", "UTF-8",
    "-deprecation",
    "-Xprint:all",
    "-unchecked", "-opt-warnings",
    "-feature",
    // "-Xfatal-warnings",
    // "-opt:l:inline", "-opt-inline-from:**",
    "-language:higherKinds",
    // "-opt:box-unbox", "-opt:nullness-tracking"
  )
}

object packages {

  lazy val versions = Map(
    "http4s" -> "0.21.0-M4",
    "typelevel" -> "2.0.0-RC2",
    "fs2" -> "1.1.0-M2",
    "circe" -> "0.12.0-RC4",
    "log4s" -> "1.8.2",
    "logback" -> "1.3.0-alpha4",
    "logstash" -> "6.2",
  )

  @inline final def packages(n: String, p: String, v: String) : Dep =
    ivy"$n::$p:$v"

  @inline final def http4s(p: String, version: String) : Dep =
    packages("org.http4s", p, version)

  @inline final def typelevel(p: String, version: String) : Dep =
    packages("org.typelevel", p, version)

  @inline final def fs2(p: String, version: String) : Dep =
    packages("co.fs2", p, version)

  @inline final def circe(p: String, version: String) : Dep =
    packages("io.circe", p, version)

  @inline final def jPackage(org: String, p: String, version: String) : Dep =
    ivy"$org:$p:$version"

  @inline final def logback(p: String, version: String) : Dep =
    jPackage("ch.qos.logback", p, version)

  lazy val http : Seq[Dep] = Seq(
    http4s("http4s-core", versions("http4s")),
    http4s("http4s-dsl", versions("http4s"))
  )

  lazy val client : Seq[Dep] = Seq(
    http4s("http4s-blaze-client", versions("http4s"))
  )

  lazy val effect : Seq[Dep] = Seq(
    typelevel("cats-core", versions("typelevel")),
    typelevel("cats-effect", versions("typelevel")),
  )

  lazy val encoder : Seq[Dep] = Seq(
    circe("circe-core", versions("circe")),
    http4s("http4s-circe", versions("http4s"))
  )

  lazy val logging : Seq[Dep] = Seq(
    packages("org.log4s", "log4s", versions("log4s")),
    logback("logback-core", versions("logback")),
    logback("logback-classic", versions("logback")),
    jPackage("net.logstash.logback", "logstash-logback-encoder", versions("logstash"))
  )

  lazy val stream : Seq[Dep] = Seq(
    fs2("fs2-core", versions("fs2")),
    fs2("fs2-io", versions("fs2"))
  )
}

object github4s extends Github4sModule {

  // @inline override final def buildInfoMembers : T[Map[String, String]] = T {
  //   Map(
  //     "version" -> publishVersion,
  //     "scalaVersion" -> scalaVersion()
  //   )
  // }

  // @inline override final def buildInfoPackageName() : Option[String] =
  //   Some("org.zerosign.github4s")

  @inline final def ivyDeps = Agg(
    (Seq(ivy"org.scala-lang:scala-reflect:${scalaVersion()}")
      ++ packages.effect
      ++ packages.stream
      ++ packages.logging
      ++ packages.http
      ++ packages.client
      ++ packages.encoder
    ):_*
  )
}
