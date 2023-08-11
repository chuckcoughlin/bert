/*
 * The settings file is used to specify which modules to include in your build.
 * There is only a single project (Bert) with multiple modules. Build order is
 * determined by dependencies.
 */

rootProject.name = "Bert"
include("common")
include("database")
include("syntax")
include("speech")
include("command")
include("control")
include("motor")
include("terminal")
include("dispatcher")
