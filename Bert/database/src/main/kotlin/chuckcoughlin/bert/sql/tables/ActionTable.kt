/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.sql.tables

import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.PoseDefinition
import chuckcoughlin.bert.common.model.RobotModel
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.*
import java.util.logging.Logger

/**
 * An action is an ordered list of poses that are executed
 * one after another.
 */
class ActionTable {
    /**
     * @return true if there is an action of the given name.
     */
    fun actionExists(cxn:Connection?,actionName:String) : Boolean {
        var result = false
        if( cxn!=null ) {
            var SQL="select * from Action where name = ?"
            var prepStatement: PreparedStatement =cxn.prepareStatement(SQL)
            var rs: ResultSet?=null
            val action=actionName.lowercase(Locale.getDefault())

            try {
                prepStatement.setQueryTimeout(10) // set timeout to 10 sec.
                prepStatement.setString(1, action)
                rs=prepStatement.executeQuery()
                while(rs.next()) {
                    val series=rs.getString("series")
                    LOGGER.info(String.format("%s.actionExists: %s is based on %s", CLSS, action,series))
                    result = true
                    break
                }
            }
            catch (e: SQLException) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                LOGGER.severe(String.format("%s.actionExists: Error (%s)", CLSS, e.message))
            }
            finally {
                if(rs != null) {
                    try {
                        rs.close()
                    }
                    catch (ignore: SQLException) { }
                }
                try {
                    prepStatement.close()
                }
                catch (ignore: SQLException) {}
            }
        }
        return result
    }
    /**
     * Delete the action.
     * @cxn an open database connection
     * @param name action name
     */
    fun deleteAction(cxn: Connection?, name: String) {
        if( cxn!=null ) {
            val action = name.lowercase(Locale.getDefault())
            var stmt=cxn.createStatement()
            try {
                val SQL=String.format("delete from Action where name = %s", action)
                stmt.execute(SQL)
            }
            finally {
                stmt.close()
            }
        }
    }
    /**
     * List the names of all defined actions.
     * @cxn an open database connection
     * @return a list of action names, comma-separate
     */
    fun getActionNames(cxn: Connection?): String {
        val names = StringBuffer()
        if( cxn!=null ) {
            val SQL = "select name from Action"
            var statement: Statement = cxn.createStatement()
            var rs: ResultSet? = null
            try {
                rs = statement.executeQuery(SQL)
                while (rs.next()) {
                    names.append(rs.getString(1))
                    names.append(", ")
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.getActionNames: Error (%s)", CLSS, e.message))
            }
            finally {
                if(rs != null) {
                    try { rs.close()}
                    catch (ignore: SQLException) {}
                }
                try {statement.close()}
                catch (ignore: SQLException) {}
            }
        }
        if( names.isNotEmpty() ) return names.substring(0, names.length - 2)
        else return "none"
    }

    /**
     * @return an ordered list of poses and delay intervals
     */
    fun getPosesForAction(cxn: Connection?,act:String) : List<PoseDefinition> {
        val list = mutableListOf<PoseDefinition>()
        return list
    }
    private val CLSS = "ActionTable"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_DATABASE)
    }

}