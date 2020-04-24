lazy val scala212 = "2.12.11"
lazy val scala213 = "2.13.2"
lazy val supportedScalaVersions = List(scala212, scala213)

name := "di4cats"

organization := "ru.dgolubets"

version in ThisBuild := "0.1.0"

scalaVersion := scala213
crossScalaVersions := List(scala212, scala213)

scalacOptions ++= {
  if (scalaVersion.value == scala213) Nil
  else Seq("-Ypartial-unification")
}

resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.bintrayRepo("dgolubets", "releases")

libraryDependencies += "org.typelevel" %% "cats-effect" % "2.1.3"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % "test"
libraryDependencies += "org.scalamock" %% "scalamock" % "4.4.0" % Test

// publishing
bintrayRepository := "releases"
bintrayOrganization in bintray := Some("dgolubets")
bintrayPackageLabels := Seq("cats", "di")
licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
homepage := Some(url("https://github.com/DGolubets/di4cats"))
bintrayVcsUrl := Some("git@github.com/DGolubets/di4cats")
publishMavenStyle := true
publishArtifact in Test := false
developers := List(Developer(
  "dgolubets",
  "Dmitry Golubets",
  "dgolubets@gmail.com",
  url("https://github.com/DGolubets")))