scalaVersion := "2.13.15"

libraryDependencies += "org.chipsalliance" %% "chisel" % "6.6.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.16" % "test"
scalacOptions ++= Seq("-language:reflectiveCalls", "-deprecation", "-feature")
addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % "6.6.0" cross CrossVersion.full)
