final object packages {
  import mill._, scalalib._

  lazy val versions = Map(
    "http4s" -> "0.21.0-M6",
    "cats-effect" -> "2.0.0",
    "fs2" -> "2.1.0",
    "circe" -> "0.12.3",
    "log4s" -> "1.8.2",
    "logback" -> "1.3.0-alpha5",
    "logstash" -> "6.3",
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
    typelevel("cats-core", versions("cats-effect")),
    typelevel("cats-effect", versions("cats-effect")),
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
