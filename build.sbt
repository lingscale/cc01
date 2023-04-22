scalaVersion := "2.13.10"

addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.6.0" cross CrossVersion.full)
libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.6.0"
// We also recommend using chiseltest for writing unit tests
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test"
scalacOptions ++= Seq("-language:reflectiveCalls", "-deprecation", "-feature")
