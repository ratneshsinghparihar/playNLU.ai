name := "realsociable machine learning service"

version := "1.0.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"
val sparkVersion = "2.1.0"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test
)


libraryDependencies += "org.apache.spark"%%"spark-core"   % sparkVersion

libraryDependencies += "org.apache.spark"%%"spark-mllib"   % sparkVersion

libraryDependencies += "org.apache.spark"%%"spark-streaming"   % sparkVersion

libraryDependencies += "org.apache.spark"%%"spark-streaming-kafka-0-10"   % sparkVersion

libraryDependencies += "org.apache.spark"%%"spark-sql-kafka-0-10"   % sparkVersion

libraryDependencies += "de.unkrig.jdisasm" % "jdisasm" % "1.0.0"

//libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" artifacts (Artifact("stanford-corenlp", "models"), Artifact("stanford-corenlp"))
libraryDependencies +="edu.stanford.nlp" % "stanford-corenlp" % "3.6.0"
libraryDependencies +="edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" % "test" classifier "models"

libraryDependencies +="com.google.protobuf" % "protobuf-java" % "2.6.1"


libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.4.8"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "MavenRepository" at "https://mvnrepository.com/"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// unmanagedJars in Compile += file("D:/backup/stanford-corenlp-full-2016-10-31/stanford-corenlp-full-2016-10-31/stanford-corenlp-3.7.0.jar")
