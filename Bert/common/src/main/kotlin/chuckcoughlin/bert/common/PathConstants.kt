/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common

import java.nio.file.Path
import java.nio.file.Paths

/**
 * These paths can be considered static constants once BERT_HOME has been set.
 * This should be done soon after startup.
 */
object PathConstants {
    var ROBOT_HOME = Paths.get(System.getProperty("user.dir")).root
    var CONFIG_PATH: Path? = null
    var DB_PATH: Path? = null
    var LOG_DIR: Path? = null
    var URDF_PATH: Path? = null

    init {
        setHome(ROBOT_HOME)
    }

    fun setHome(home: Path) {
        ROBOT_HOME = home
        CONFIG_PATH = Paths.get(ROBOT_HOME.toFile().absolutePath, "etc", "bert.xml")
        DB_PATH = Paths.get(ROBOT_HOME.toFile().absolutePath, "etc", "bert.db")
        LOG_DIR = Paths.get(ROBOT_HOME.toFile().absolutePath, "logs")
        URDF_PATH = Paths.get(ROBOT_HOME.toFile().absolutePath, "etc", "urdf.xml")
    }
}