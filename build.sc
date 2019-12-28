import $file.plugins
import $file.deps

import mill._, scalalib._, publish._
import contrib.BuildInfo

import $ivy.`org.eclipse.jgit:org.eclipse.jgit:5.5.1.201910021850-r`

final object Globals {
  import java.nio.file.Path
  import org.eclipse.jgit.api.Git

  @inline final val version : String =
    Git.open(Path.of("./")
      .toAbsolutePath.toFile).describe()
      .setTags(true).call
}

trait Github4sModule extends ScalaModule {

  // HACK: hack to enable modules outside root folder
  //       this will only modify locally the instance of this trait
  //       not global ScalaModule
  //
  override def millSourcePath: os.Path = {
    val parent = os.Path.apply(super.millSourcePath.toNIO.getParent)
    val child = super.millSourcePath.last
    parent / "modules" / child
  }

  @inline final def publishVersion = Globals.version

  @inline final def scalaVersion = "2.13.1"

  @inline final def scalacOptions = Seq(
    // "-P:acyclic:force",
    "-target:1.12",
    "-encoding", "UTF-8",
    "-deprecation",
    // "-Xprint:all",
    "-unchecked",
    "-deprecation",
    "-opt-warnings",
    "-feature",
    // "-opt:l:inline", "-opt-inline-from:**",
    "-language:higherKinds",
    // "-opt:box-unbox", "-opt:nullness-tracking"
  )
}

object data extends Github4sModule {
  import deps.packages

  @inline final def ivyDeps = Agg(
    (Seq(ivy"org.scala-lang:scala-reflect:${scalaVersion()}")
      ++ packages.effect
      ++ packages.encoder
    ):_*
  )

}

object client extends Github4sModule {

  import deps.packages

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
