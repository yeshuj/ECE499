// See README.md for license details.

ThisBuild / scalaVersion     := "2.12.13"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "com.github.yeshuj"

lazy val root = (project in file("."))
  .settings(
    name := "ECE499",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % "3.4.3",
      "edu.berkeley.cs" %% "chiseltest" % "0.3.3" % "test",
      "edu.berkeley.cs" %% "chisel-iotesters" % "1.5+" % "test"
    ),
    scalacOptions ++= Seq(
      "-Xsource:2.11",
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      // Enables autoclonetype2 in 3.4.x (on by default in 3.5)
      "-P:chiselplugin:useBundlePlugin"
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.4.3" cross CrossVersion.full),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
//    libraryDependencies ++= Seq(
//      "junit" % "junit" % "4.13",
//      "org.scalatest" %% "scalatest" % "3.2.2",
//      "org.scalatestplus" %% "scalacheck-1-14" % "3.1.1.1",
//      "org.scalacheck" %% "scalacheck" % "1.14.3",
//      "com.github.scopt" %% "scopt" % "3.7.1"
//    )

  )
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "3")
