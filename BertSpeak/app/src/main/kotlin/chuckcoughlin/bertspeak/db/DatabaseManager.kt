/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.db

import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.FileUtils
import chuckcoughlin.bertspeak.common.NameValue
import java.io.File


/**
 * Persistent application parameters are stored in a SQLite database. We check for
 * existence and version when the database is first opened. Beyond that the
 * checks are ignored. Create a separate instance of this class wherever needed.
 * The database is a Singleton (object) and is closed after each transaction.
*/

class DatabaseManager {
    fun execSQL(sql: String?) {
        val database = getWritableDatabase()
        database.execSQL(sql)
    }

    /**
     * Trap and ignore any errors.
     * @param sqLiteDatabase
     * @param sql
     */
     fun execLenient(sql: String?) {
        try {
            execSQL(sql)
        }
        catch (sqle: SQLException) {
            Log.i(CLSS, String.format("execLenient:%s: SQLException ignored (%s)",
                sql,sqle.localizedMessage))
        }
    }
    /**
     * Must be called when the manager is first created. If the database already exists on disk
     * with the same name, this method will have no effect.
     */
    fun initialize() {
        val SQL = StringBuilder()
        SQL.append("CREATE TABLE IF NOT EXISTS Settings (")
        SQL.append("  name TEXT PRIMARY KEY,")
        SQL.append("  value TEXT DEFAULT '',")
        SQL.append("  hint TEXT DEFAULT 'hint'")
        SQL.append(")")
        execSQL(SQL.toString())
        // Add initial settings - fail silently if they exist. The default values make sense
        // for development.
        var statement =
            String.format("INSERT INTO Settings(Name,Value,Hint) VALUES(\'%s\',\'%s\',\'%s\')",
                BertConstants.BERT_PAIRED_DEVICE,
                BertConstants.BERT_PAIRED_DEVICE_HINT,
                BertConstants.BERT_PAIRED_DEVICE_HINT)
        execLenient(statement)

        statement =
            String.format("INSERT INTO Settings(Name,Value,Hint) VALUES(\'%s\',\'%s\',\'%s\')",
                BertConstants.BERT_SIMULATED_CONNECTION,"true",
                BertConstants.BERT_SIMULATED_CONNECTION_HINT)
        execLenient(statement)
        Log.i(CLSS,String.format("onCreate: Guarantee settings exist in %s at %s",
            BertConstants.DB_NAME,BertConstants.DB_FILE_PATH))
    }
    // ================================================ Settings =============================
    /**
     * Read name/value pairs from the database.
     */
    fun getSetting(name: String): String? {
        synchronized(DB) {
            var result: String? = null
            val database = getReadableDatabase()
            val args = arrayOfNulls<String>(1) // Use for PreparedStatement
            args[0] = name
            val SQL = "SELECT value FROM Settings WHERE Name=?"
            val cursor = database.rawQuery(SQL, args)
            cursor.moveToFirst()
            if (!cursor.isAfterLast) {
                result = cursor.getString(0)
                Log.i(CLSS, String.format("getSetting: %s = %s", name, result))
            }
            cursor.close()
            database.close()
            return result
        }
    }

    /**
     * Read name/value pairs from the database.
     */
    fun getSettings (): List<NameValue> {
        synchronized(DB) {
            val list: MutableList<NameValue> = ArrayList()
            val database = getReadableDatabase()
            val args = arrayOfNulls<String>(0) // Use for PreparedStatement
            val SQL = "SELECT name,value,hint FROM Settings ORDER BY Name"
            val cursor = database.rawQuery(SQL, args)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val nv = NameValue(cursor.getString(0), cursor.getString(1), cursor.getString(2))
                Log.i(CLSS, String.format("getSettings: %s = %s (%s)", nv.name, nv.value, nv.hint))
                list.add(nv)
                cursor.moveToNext()
            }
            cursor.close()
            database.close()
            return list
        }
    }

    /**
     * Save a single setting to the database.
     * @param nv the subject name-value pair
     */
    fun updateSetting(nv: NameValue?) {
        synchronized(DB) {
            val database = getWritableDatabase()
            val SQL = "UPDATE Settings set value=?, hint=? WHERE name = ?"
            val bindArgs = arrayOfNulls<String>(3)
            bindArgs[0] = nv!!.value
            bindArgs[1] = nv.hint
            bindArgs[2] = nv.name
            database.execSQL(SQL, bindArgs)
            database.close()
        }
    }

    /**
     * Save settings to the database
     * @param items a list of name-value pairs
     */
    fun updateSettings(items: List<NameValue>) {
        synchronized(DB) {
            val database = getWritableDatabase()
            val SQL = "UPDATE Settings set value=?, hint=? WHERE name = ?"
            val bindArgs = arrayOfNulls<String>(3)
            val count = items.size
            var index = 0
            while (index < count) {
                val nv = items[index]
                bindArgs[0] = nv.value
                bindArgs[1] = nv.hint
                bindArgs[2] = nv.name
                database.execSQL(SQL, bindArgs)
                index++
            }
            database.close()
        }
    }

    companion object DB {
        private const val CLSS = "DatabaseManager"
        @Volatile
        private var dbobject: SQLiteDatabase? = null

        // If the database is open, close it to ensure correct writable flags
        private fun getDatabase(canWrite:Boolean) : SQLiteDatabase {
            if( dbobject!=null && dbobject!!.isOpen) dbobject!!.close()
            val OS = System.getProperty("os.name", "generic")!!.lowercase()
            // For Linux-like systems (simulation/test mode), use a fixed path
            if (OS.contains("mac") &&
                OS.contains("nux")   ) {
                val success = FileUtils.ensureFileExists(BertConstants.DB_FILE_PATH)
                val builder = SQLiteDatabase.OpenParams.Builder()
                builder.addOpenFlags(SQLiteDatabase.CREATE_IF_NECESSARY)
                if( canWrite )  builder.addOpenFlags(SQLiteDatabase.OPEN_READWRITE)
                else            builder.addOpenFlags(SQLiteDatabase.OPEN_READONLY)
                val params = builder.build()
                val path = File(BertConstants.DB_FILE_PATH)
                dbobject = SQLiteDatabase.openDatabase(path,params)
            }
            // For Android, just use the database name
            else {
                var flags = SQLiteDatabase.CREATE_IF_NECESSARY
                flags = if( canWrite ) flags or SQLiteDatabase.OPEN_READWRITE
                else flags or SQLiteDatabase.OPEN_READONLY
                dbobject = SQLiteDatabase.openDatabase(
                    BertConstants.DB_FILE_PATH, null,flags)
            }
            configureDatabase(dbobject!!)

            return dbobject!!
        }
        fun getReadableDatabase() : SQLiteDatabase {
        return getDatabase(false)
    }
    fun getWritableDatabase() : SQLiteDatabase {
        return getDatabase(true)
    }
    private fun configureDatabase(db:SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }
}
// ================= End of DB =======================

}
