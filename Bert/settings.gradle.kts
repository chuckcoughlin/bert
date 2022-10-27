/*
 * The settings file is used to specify which projects and modules to include in your build.
 *
 * There is only a single project (Bert) with multiple modules.
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
include("syntax")
