/*
 * The settings file is used to specify which projects and modules to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/7.5/userguide/multi_project_builds.html
 */

rootProject.name = "Bert"
include("common")
include("command")
include("control")
include("database")
include("dispatcher")
include("motor")
include("terminal")
include("speech")
