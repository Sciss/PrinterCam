lazy val baseName       = "PinterCam"
//lazy val baseNameL      = baseName.toLowerCase
lazy val projectVersion = "0.1.0-SNAPSHOT"
lazy val gitHost        = "codeberg.org"
lazy val gitUser        = "sciss"
lazy val gitRepo        = baseName

lazy val commonSettings = Seq(
  version      := projectVersion,
  homepage     := Some(url(s"https://$gitHost/$gitUser/$gitRepo")),
  scalaVersion := "3.2.1",
  scalacOptions ++= Seq("-deprecation"),
  licenses     := Seq("AGPL v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")),
  run / fork   := true,
  libraryDependencies ++= Seq(
//    "com.softwaremill.sttp.client3" %% "core"       % deps.common.sttp,       // HTTP client
    "de.sciss"                      %% "fileutil"   % deps.common.fileUtil,   // utility functions
    "de.sciss"                      %% "numbers"    % deps.common.numbers,    // numeric utilities
//    "org.jsoup"                     %  "jsoup"      % deps.common.jsoup,      // HTML parsing
    "org.rogach"                    %% "scallop"    % deps.common.scallop,    // command line option parsing
    "de.sciss"                      %% "swingplus"  % deps.common.swingPlus,  // user interface
  )
)

lazy val root = project.in(file("."))
  .settings(commonSettings)
  .settings(
    name := baseName,
    description  := "Using an Amcrest IP camera to monitor my 3D printer"
  )

lazy val deps = new {
  val common = new {
    val fileUtil        = "1.1.5"
    val jsoup           = "1.15.3"
    val numbers         = "0.2.1"
    val scallop         = "4.1.0"
    val sttp            = "3.8.9"
    val swingPlus       = "0.5.0"
  }
}
