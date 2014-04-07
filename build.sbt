name := "play-stratego"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  filters,
  cache,
  "org.springframework.scala" %% "spring-scala" % "1.0.0.RC1",
  "org.springframework" % "spring-context" % "4.0.3.RELEASE",
  "javax.inject" % "javax.inject" % "1",
  "org.springframework.security" % "spring-security-web" % "3.2.3.RELEASE",
  "org.springframework.security" % "spring-security-config" % "3.2.3.RELEASE",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.3.2",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.3.2",
  "org.springframework.data" % "spring-data-mongodb" % "1.4.1.RELEASE",
  "com.github.nscala-time" %% "nscala-time" % "0.8.0",
  "org.mongodb" % "mongo-java-driver" % "2.11.4",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.6",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.6"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.3" % "test"
)

play.Project.playScalaSettings

scalacOptions ++= Seq("-feature", "-language:postfixOps")

resolvers += "SpringSource Milestone Repository" at "http://repo.springsource.org/milestone"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases/"
