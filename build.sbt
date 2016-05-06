organization  := "com.zhranklin.scripts"

version       := "0.1"

scalaVersion  := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  Seq(
    "org.jsoup" % "jsoup" % "1.9.1",
    "com.google.code.gson" % "gson" % "2.6.2"
  )
}

assemblyJarName in assembly := "petest.jar"
mainClass in assembly := Some("com.zhranklin.Petest")
