/**
 * Copyright 2024-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.sql.tables

import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.common.util.TextUtility
import chuckcoughlin.bert.sql.db.Database.getPoseIdForName
import chuckcoughlin.bert.sql.db.SQLConstants
import chuckcoughlin.bert.sql.db.SQLConstants.SQL_NULL_CONNECTION
import com.google.gson.GsonBuilder
import java.sql.*
import java.util.*
import java.util.logging.Logger

/**
 * Handle the storage and retrieval of parameters for recognized faces.
 * This class serves as a Kotlin interface to the Face, FaceLandmark and FaceContour tables. It provides
 * methods for finding and reading a face for purposes of comparison
 */
class FaceTable {

    /**
     * Add new face details to the database. For now use just the landmarks.
     * If the name alrady exists, then update the existing details.
     */
    fun createFace(cxn: Connection?, facename:String,details: FacialDetails ) {
        if( cxn!=null ) {
            var statement: Statement? = null
            val name = facename.lowercase()
            var faceid = getFaceIdForName(cxn,name)
            LOGGER.info(String.format("%s.createFace: %s is id %d", CLSS,name,faceid))
            statement = cxn.createStatement()
            if( faceid == SQLConstants.NO_FACE ) {
                faceid = getNextFaceId(cxn)
                val SQL = String.format("insert into Face(name,faceid) values('%s',%d)",name,faceid)
                if(DEBUG) LOGGER.info(String.format("%s.createFace: executing %s)", CLSS, SQL))
                statement.executeUpdate(SQL)
            }
            else {
                // Face exists, so delete any existing data from FaceContour and FaceLandmark tables
                var SQL = String.format("delete from FaceContour where faceid = %d",faceid)
                if(DEBUG) LOGGER.info(String.format("%s.createFace: executing %s)", CLSS, SQL))
                statement.executeUpdate(SQL)
                SQL = String.format("delete from FaceLandmark where faceid = %d",faceid)
                if(DEBUG) LOGGER.info(String.format("%s.createFace: executing %s)", CLSS, SQL))
                statement.executeUpdate(SQL)
            }

            try {
                // Define the face as a collection of landmarks
                for (landmarkName in details.landmarks.keys) {
                    val point=details.landmarks.get(landmarkName)!!
                    val SQL = String.format("insert into FaceLandmark(faceid,landmarkCode,x,y) values(%d,'%s',%2.4f,%2.4f)",
                            faceid,landmarkName,point.x,point.y)
                    if(DEBUG) LOGGER.info(String.format("%s.createPose: executing %s)", CLSS, SQL))
                    statement.executeUpdate(SQL)
                }
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.createFace: Error (%s)", CLSS, e.message))
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
    /**
     * Delete the face and associated landmarks.
     * @cxn an open database connection
     * @param name face name
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
                LOGGER.severe(String.format("%s.getFaceIdForName: Error (%s)",CLSS, e.message))
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
                    SQL=String.format("delete from Face where faceid = %d", faceid)
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
     * Find the face id given face name. If the face does not
     * exist, return NO_POSE. The name is always stored in lower case.
     * @cxn an open database connection
     * @param name user entered string
     * @return the corresponding face id if it exists, otherwise NO_FACE
     */
    fun getFaceIdForName(cxn: Connection?, name: String): Long {
        var faceid: Long = SQLConstants.NO_FACE
        if( cxn!=null ) {
            val facename = name.lowercase(Locale.getDefault())
            var SQL = String.format("select faceid from Face where name = '%s'",facename)
            val statement = cxn.createStatement()
            var rs: ResultSet? = null
            try {
                rs = statement.executeQuery(SQL)
                while (rs.next()) {
                    faceid = rs.getLong("faceid")
                    LOGGER.info(String.format("%s.getFaceIdForName: %s is %d", CLSS, facename,faceid))
                    break
                }
                rs.close()
            }
            catch (e: SQLException) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                LOGGER.severe(String.format("%s.getFaceIdForName: Error (%s)", CLSS, e.message))
            }
            finally {
                if(rs != null) {
                    try {
                        rs.close()
                    }
                    catch (ignore: SQLException) {}
                }
                try {statement.close()}
                catch (ignore: SQLException) {}
            }
        }
        return faceid
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
        val names = mutableListOf<String>()
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
        return TextUtility.createTextForSpeakingFromList(names)
    }
    /**
     * Find an unused face id (one larger than the current maximum).
     * @cxn an open database connection
     * @return the corresponding pose name if it exists, otherwise NULL
     */
    private fun getNextFaceId(cxn: Connection?): Long {
        var faceid: Long = 1
        if( cxn!=null ) {
            var SQL = "select max(faceid) from Face"
            var statement: Statement = cxn.createStatement()
            var rs: ResultSet? = null

            try {
                rs = statement.executeQuery(SQL)
                while (rs.next()) {
                    faceid = rs.getLong(1)
                    faceid = faceid + 1
                    break
                }
                rs.close()
            }
            catch (e: SQLException) {
                LOGGER.severe(String.format("%s.getNextFaceId: Error (%s)", CLSS, e.message))
            }
            finally {
                if (rs != null) {
                    try {
                        rs.close()
                    }
                    catch (ignore: SQLException) {
                    }
                }
                try {
                    statement.close()
                }
                catch (ignore: SQLException) {
                }
            }
        }
        return faceid
    }
    /**
     * Compare details from the database to the supplied object.
     * @return true if the comparison scores less than the tolerance
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
                    LOGGER.info(String.format("%s.idMatchesDetails: Computed error: %3.2f vs %3.2f", CLSS, err,TOLERANCE))
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


    private val CLSS = "FaceTable"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG: Boolean
    private val TOLERANCE = 0.5

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_DATABASE)
    }
}