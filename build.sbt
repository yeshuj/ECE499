// See README.md for license details.

//ThisBuild / scalaVersion     := "2.12.13"
//ThisBuild / version          := "0.1.0"
//ThisBuild / organization     := "com.github.yeshuj"

organization := "com.github.yeshuj"
version := "0.1.0"
name := "ECE499"
scalaVersion := "2.12.10"


//lazy val root = (project in file("."))
//  .settings(
//    name := "ECE499",
//    libraryDependencies ++= Seq(
//      "edu.berkeley.cs" %% "chisel3" % "3.4.3",
//      "edu.berkeley.cs" %% "rocketchip" % "1.2.+",
//      "edu.berkeley.cs" %% "chisel-iotesters" % "1.5+"
//    ),
//    scalacOptions ++= Seq(
//      "-Xsource:2.11",
//      "-language:reflectiveCalls",
//      "-deprecation",
//      "-feature",
//      "-Xcheckinit",
//      // Enables autoclonetype2 in 3.4.x (on by default in 3.5)
//      "-P:chiselplugin:useBundlePlugin"
//    ),
//    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.4.3" cross CrossVersion.full),
//    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
//    resolvers ++= Seq(
//      Resolver.sonatypeRepo("snapshots"),
//      Resolver.sonatypeRepo("releases"),
//      Resolver.mavenLocal)
//  )