/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.sql.tables

import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.sql.db.SQLConstants
import chuckcoughlin.bert.sql.db.SQLConstants.SQL_NULL_CONNECTION
import com.google.gson.GsonBuilder
import java.sql.*
import java.util.*
import java.util.logging.Logger

/**
 * Handle the storage and retrieval of parameters for recognized faces.
 * This class serves as a Kotlin interface to the Face and FaceContour tables. It provides
 * methods for finding and reading a face for purposes of comparison
 */
class FaceTable {

    /**
     * Delete the pose and associated joint details.
     * @cxn an open database connection
     * @param name pose name
     */
    fun deleteFace(cxn: Connection?, facename: String) {
        if( cxn!=null ) {
            var SQL = "select faceid from Face where name = ?"
            var statement = cxn.prepareStatement(SQL)
            var rs: ResultSet? = null
            val name = facename.lowercase(Locale.getDefault())
            var faceid = SQLConstants.NO_FACE

            try {
                statement.setQueryTimeout(10) // set timeout to 10 sec.
                statement.setString(1, name)
                rs = statement.executeQuery()
                while (rs.next()) {
                    faceid = rs.getLong("faceid")
                    LOGGER.info(String.format("%s.deleteFace: %s is %d", CLSS, name, faceid))
                    break
                }
                if( faceid== SQLConstants.NO_FACE ) return   // Didn't exist
            }
            catch (e: SQLException) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                LOGGER.severe(String.format("%s.getPoseIdForName: Error (%s)",CLSS, e.message))
            }
            finally {
                if(rs != null) {
                    try {
                        rs.close()
                        statement.close()
                    }
                    catch (ignore: SQLException) {}
                }
                // Now do the deletions
                var stmt=cxn.createStatement()
                try {
                    SQL=String.format("delete from FaceName where faceid = %d", faceid)
                    stmt.execute(SQL)
                    SQL=String.format("delete from FaceLandmark where faceid = %d", faceid)
                    stmt.execute(SQL)
                    SQL=String.format("delete from FaceContour where faceid = %d", faceid)
                    stmt.execute(SQL)
                }
                finally {
                    stmt.close()
                }
            }
        }
    }
    /**
     * @return true if there is a face of the given name.
     */
    fun faceExists(cxn:Connection?,faceName:String) : Boolean {
        var result = false
        if( cxn!=null ) {
            var SQL="select faceId from Face where name = ?"
            var prepStatement: PreparedStatement =cxn.prepareStatement(SQL)
            var rs: ResultSet?=null
            val name=faceName.lowercase(Locale.getDefault())

            try {
                prepStatement.setQueryTimeout(10) // set timeout to 10 sec.
                prepStatement.setString(1, name)
                rs=prepStatement.executeQuery()
                while(rs.next()) {
                    LOGGER.info(String.format("%s.faceExists: %s", CLSS,name))
                    result = true
                    break
                }
            }
            catch (e: SQLException) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                LOGGER.severe(String.format("%s.faceExists: Error (%s)", CLSS, e.message))
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
     * @return the names of faces in JSON formatted string
     */
    fun faceNamesToJSON(cxn:Connection?) : String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        var names = mutableListOf<String>()
        if( cxn!=null ) {
            val SQL = "select name from Face"
            var statement: Statement = cxn.createStatement()
            var rs: ResultSet? = null
            try {
                rs = statement.executeQuery(SQL)
                while (rs.next()) {
                    names.add(rs.getString(1))
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.faceNamesToJson: Error (%s)", CLSS, e.message))
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
        return gson.toJson(names)
    }

    /**
     * Given a faceId, retrieve the name corresponding to a faceId
     * @cxn an open database connection
     * @faceId
     * @return the name
     */
    fun getFaceName(cxn: Connection?,id:Long): String {
        var name = ConfigurationConstants.NO_NAME
        if( cxn!=null ) {
            val SQL = String.format("select name from Face where FaceId = %d",id)
            var statement: Statement = cxn.createStatement()
            var rs: ResultSet? = null
            try {
                rs = statement.executeQuery(SQL)
                while (rs.next()) {
                    name = rs.getString(1)
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.getFaceName: Error (%s)", CLSS, e.message))
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
        return name
    }

    /**
     * List the owners of all known faces
     * @cxn an open database connection
     * @return a list of pose names, comma-separate
     */
    fun getFaceNames(cxn: Connection?): String {
        val names = StringBuffer()
        if( cxn!=null ) {
            val SQL = "select name from Face"
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
                LOGGER.severe(String.format("%s.getFaceNames: Error (%s)", CLSS, e.message))
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
     * Match the details from the database to the supplied object.
     */
    fun idMatchesDetails(cxn: Connection?,id:Long,details:FacialDetails) : Boolean {
        var result = false
        if( cxn!=null ) {
            var statement: Statement = cxn.createStatement()
            var rs: ResultSet? = null
            val SQL1 = String.format(
                    "select contourcode,indx,x,y from FaceContour where faceid=%d order by contourcode,indx",id)
            val SQL2 = String.format(
                    "select landmarkcode,x,y from FaceLandmark where faceid=%d order by landmarkcode",id)
            try {
                // For starters simply compare landmarks
                rs = statement.executeQuery(SQL2)
                var err = 0.0
                var count = 0
                while (rs.next()) {
                    val name = rs.getString(1)
                    val x = rs.getDouble(2)
                    val y = rs.getDouble(3)
                    val pnt = details.landmarks[name]
                    if( pnt!=null ) {
                        count = count+1
                        err = err + (x-pnt.x)*(x-pnt.x) + (y-pnt.y)*(y-pnt.y)
                    }
                }
                if( count>0 ) {
                    err = Math.sqrt(err/(2.0*count))
                    LOGGER.info(String.format("%s.idMatchesDetails: Computed error: %0.2f vs %0.2f", CLSS, err,TOLERANCE))
                    if( err<TOLERANCE ) result = true
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.idMatchesDetails: Error (%s)", CLSS, e.message))
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
        return result
    }
    /**
     * Associate a known face with the supplied facial detection details.
     * @cxn an open database connection
     * @param details facial detection details
     * @return the face_id of the match, else NO_FACE
     */
    fun matchDetailsToFace(cxn: Connection?, details: FacialDetails) : Long {
        var face_id = SQLConstants.NO_FACE
        if( cxn!=null ) {
            val SQL = "select faceid from Face"
            var statement: Statement = cxn.createStatement()
            var rs: ResultSet?
            try {
                rs = statement.executeQuery(SQL)
                while (rs.next()) {
                    val id = rs.getLong(1)
                    if( idMatchesDetails(cxn,id,details) ) {
                        face_id = id
                        break
                    }
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.mapFaceNameToDetails: Database error (%s)", CLSS, e.message))
            }
            finally {
                try {
                    statement.close()
                }
                catch (ignore: SQLException) {}
            }
        }
        return face_id
    }

    /**
     * Save a list of motor position values as a pose. Assign the pose a name equal to the
     * id of the new database record.
     * @param mcmap contains a map of motor configurations with positions that define the pose.
     * @return the new record id as a string.
     */
    fun saveJointLocationsAsNewPose(cxn: Connection?, map: Map<Joint, MotorConfiguration>): String {
        LOGGER.info(String.format("%s.saveJointLocationsAsNewPose:", CLSS))
        var id: Long = SQL_NULL_CONNECTION
        if( cxn!=null ) {
            var statement: Statement? = null
            var prep: PreparedStatement? = null

            try {
                val sb = StringBuffer("INSERT INTO Pose (name,parameter")
                val valuesBuffer = StringBuffer("VALUES ('NEWPOSE','position'")
                for (mc in map.values) {
                    sb.append(",")
                    sb.append(mc.joint.name)
                    valuesBuffer.append(",?")
                }
                var SQL = sb.append(") ").append(valuesBuffer).append(")").toString()
                LOGGER.info(String.format("%s.saveJointLocationsAsNewPose:\n%s", CLSS, SQL))
                prep = cxn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)
                var index = 1
                for (mc in map.values) {
                    prep.setInt(index, mc.angle.toInt())
                    index++
                }
                prep.executeUpdate()
                val generatedKeys: ResultSet = prep.getGeneratedKeys()
                if (generatedKeys.next()) {
                    id = generatedKeys.getLong(1)
                }
                SQL = "UPDATE Pose Set name = id WHERE name='NEWPOSE'"
                statement = cxn.createStatement()
                statement.execute(SQL)
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.saveJointLocationsAsNewPose: Error (%s)", CLSS, e.message))
            }
            finally {
                if (prep != null) {
                    try {
                        prep.close()
                    }
                    catch (ignore: SQLException) {
                    }
                }
                if (statement != null) {
                    try {
                        statement.close()
                    }
                    catch (ignore: SQLException) {
                    }
                }
            }
        }
        return id.toString()
    }

    /**
     * Save a list of motor position values as a pose. Try an update first. If no rows are affected
     * then do an insert.
     * @param mcmap contains a map of motor configurations. Joints not in the list are ignored.
     * @param pz name
     */
    fun saveJointLocationsForPose(cxn: Connection?, map: Map<Joint, MotorConfiguration>, pz: String) {
        if( cxn!=null ) {
            LOGGER.info(String.format("%s.saveJointAnglesForPose: %s)", CLSS, pz))
            var statement: PreparedStatement? = null
            val pose = pz.lowercase(Locale.getDefault())
            var SQL = StringBuffer("UPDATE Pose SET ")
            var index = 0
            for (mc in map.values) {
                index++
                SQL.append(
                    java.lang.String.format("\n'%s'=%.1f%s",
                        mc.joint.name, mc.angle,
                        if (index == map.size) "" else ","
                    )
                )
            }
            SQL.append("\nWHERE name=? AND parameter='position';")
            try {
                LOGGER.info(String.format("%s.saveJointAnglesForPose: \n%s)", CLSS, SQL.toString()))
                statement = cxn.prepareStatement(SQL.toString())
                statement.setString(1, pose)
                statement.executeUpdate()
                if (statement.getUpdateCount() == 0) {
                    // There was nothing to update. Do an insert. This will auto-increment primary key.
                    statement.close()
                    SQL = StringBuffer("INSERT INTO Pose (name,parameter")
                    val valuesBuffer = StringBuffer("VALUES (?,'position'")
                    for (mc in map.values) {
                        SQL.append(",")
                        SQL.append(mc.joint.name)
                        valuesBuffer.append(",?")
                    }
                    SQL.append(") ").append(valuesBuffer).append(")").toString()
                    LOGGER.info(String.format("%s.saveJointAnglesForPose: \n%s)", CLSS, SQL.toString()))
                    statement = cxn.prepareStatement(SQL.toString())
                    statement.setString(1, pose)
                    index = 2
                    for (mc in map.values) {
                        statement.setInt(index, mc.angle.toInt())
                        index++
                    }
                    statement.executeUpdate()
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.saveJointAnglesForPose: Database error (%s)", CLSS, e.message))
            }
            finally {
                if (statement != null) {
                    try {
                        statement.close()
                    }
                    catch (ignore: SQLException) {
                    }
                }
            }
        }
    }
    private val CLSS = "FaceTable"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean
    private val TOLERANCE = 0.5

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_DATABASE)
    }
}