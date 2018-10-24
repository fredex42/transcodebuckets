import NativePackagerHelper._
import RpmConstants._

name := "transcodebuckets"
 
version := "1.0" 
      
lazy val `transcodebuckets` = (project in file(".")).enablePlugins(PlayScala, RiffRaffArtifact, SystemdPlugin)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice )

unmanagedResourceDirectories in Test +=  (baseDirectory ( _ /"target/web/public/test" )).value

val amazonSdkVersion = "1.11.401"
libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-dynamodb" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-elastictranscoder" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-sns" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-sqs" % amazonSdkVersion
)

val circeVersion = "0.9.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.16" % Test

libraryDependencies ++= Seq(
  "com.gu" %% "scanamo" % "1.0.0-M6"
)

//Generic Linux package build configuration
mappings in Universal ++= directory("postrun/")

packageSummary in Linux := "A system to manage, backup and archive multimedia project files"

packageDescription in Linux := "A system to manage, backup and archive multimedia project files"

debianPackageDependencies := Seq("openjdk-8-jre-headless")
serverLoading in Debian := Some(ServerLoader.Systemd)
serviceAutostart in Debian := false

version in Debian := s"${version.value}-${sys.env.getOrElse("CIRCLE_BUILD_NUM","SNAPSHOT")}"
name in Debian := "transcodebuckets"

maintainer := "Andy Gallagher <andy.gallagher@theguardian.com>"
packageSummary := "Tool to ensure that holding pen media is proxies and re-attached"
packageDescription := """Tool to ensure that holding pen media is proxies and re-attached"""


riffRaffPackageType := (packageBin in Debian).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffManifestBranch := sys.env.getOrElse("BRANCH","unknown")
riffRaffManifestRevision := sys.env.getOrElse("BUILD_NUMBER","SNAPSHOT")
riffRaffManifestVcsUrl := sys.env.getOrElse("BUILD_URL", "")
riffRaffBuildIdentifier := sys.env.getOrElse("BUILD_NUMBER", "SNAPSHOT")
riffRaffPackageName := "transcodebuckets"
riffRaffManifestProjectName := "multimedia:transcodebuckets"