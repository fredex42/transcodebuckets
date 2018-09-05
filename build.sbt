name := "transcodebuckets"
 
version := "1.0" 
      
lazy val `transcodebuckets` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice )

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

val amazonSdkVersion = "1.11.401"
libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-dynamodb" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-elastictranscoder" % amazonSdkVersion,
  "com.amazonaws" % "aws-java-sdk-sns" % amazonSdkVersion
)