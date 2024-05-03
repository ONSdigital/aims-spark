resolvers ++= Seq(
  // allows us to include spark packages
  "spark-packages" at "https://repos.spark-packages.org/",
  "conjars" at "https://conjars.org/repo"
)

val localTarget: Boolean = false
// set to true when testing locally (or to build a fat jar)
// false for deployment to Cloudera with a thin jar
// reload all sbt projects to clear ivy cache

val localDeps = Seq(
  "org.apache.spark" %% "spark-core" % "3.5.1",
  "org.apache.spark" %% "spark-sql" % "3.5.0",
  "org.apache.spark" %% "spark-hive" % "3.5.1"
)

val clouderaDeps = Seq(
  "org.apache.spark" %% "spark-core" % "3.5.1" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-hive" % "3.5.1" % "provided",
  "org.apache.httpcomponents" % "httpclient" % "4.5.14"
)

val otherDeps = Seq(
  "com.typesafe" % "config" % "1.4.3",
  "org.elasticsearch" %% "elasticsearch-spark-30" % "8.13.2" excludeAll ExclusionRule(organization = "javax.servlet"),
  "org.rogach" %% "scallop" % "5.1.0",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "com.crealytics" %% "spark-excel" % "3.5.0_0.20.3",
  "org.scalatest" %% "scalatest" % "3.2.18" % Test
)

if (localTarget) libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
) ++ localDeps ++ otherDeps
else libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
) ++ clouderaDeps ++ otherDeps

dependencyOverrides += "commons-codec" % "commons-codec" % "1.15"

scalacOptions ++= List("-unchecked", "-Xlint")