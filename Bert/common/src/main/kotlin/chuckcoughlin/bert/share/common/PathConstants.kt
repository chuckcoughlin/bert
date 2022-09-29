/**
 * Copyright 2018. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.share.common

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
        chuckcoughlin.bert.share.common.PathConstants.setHome(chuckcoughlin.bert.share.common.PathConstants.ROBOT_HOME)
    }

    fun setHome(home: Path) {
        chuckcoughlin.bert.share.common.PathConstants.ROBOT_HOME = home
        chuckcoughlin.bert.share.common.PathConstants.CONFIG_PATH = Paths.get(chuckcoughlin.bert.share.common.PathConstants.ROBOT_HOME.toFile().absolutePath, "etc", "bert.xml")
        chuckcoughlin.bert.share.common.PathConstants.DB_PATH = Paths.get(chuckcoughlin.bert.share.common.PathConstants.ROBOT_HOME.toFile().absolutePath, "etc", "bert.db")
        chuckcoughlin.bert.share.common.PathConstants.LOG_DIR = Paths.get(chuckcoughlin.bert.share.common.PathConstants.ROBOT_HOME.toFile().absolutePath, "logs")
        chuckcoughlin.bert.share.common.PathConstants.URDF_PATH = Paths.get(chuckcoughlin.bert.share.common.PathConstants.ROBOT_HOME.toFile().absolutePath, "etc", "urdf.xml")
    }
}