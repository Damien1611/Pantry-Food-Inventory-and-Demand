scalaVersion := "3.8.4"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Wunused:all",
  "-Wconf:msg=Implicit parameters:s"
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "project_23080633",
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "21.0.0-R32"
    )
  )
