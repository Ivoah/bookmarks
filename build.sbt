ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.1"

ThisBuild / assemblyMergeStrategy := {
  case PathList("module-info.class") => MergeStrategy.discard
  case x if x.endsWith("/module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val root = (project in file("."))
  .settings(
    name := "bookmarks",
    idePackagePrefix := Some("net.ivoah.bookmarks"),
    libraryDependencies ++= Seq(
      "net.ivoah" %% "vial" % "0.3.3",
      "com.lihaoyi" %% "scalatags" % "0.12.0",
      "org.rogach" %% "scallop" % "5.1.0"
    ),
    assembly / assemblyOutputPath := file("bookmarks.jar")
  )
