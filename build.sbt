libraryDependencies ++= Seq(
  "net.databinder" %% "dispatch-core" % "0.8.7",
  "net.databinder" %% "dispatch-http-json" % "0.8.7",
  "net.databinder" %% "dispatch-http" % "0.8.7" % "test",
  "org.specs2" %% "specs2" % "1.7.1" % "test"
)

resolvers ++= Seq("releases" at "http://scala-tools.org/repo-releases")