/**
 * Copyright 2023-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.db

import android.database.DatabaseErrorHandler
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import chuckcoughlin.bertspeak.common.ConfigurationConstants
import chuckcoughlin.bertspeak.common.NameValue
import chuckcoughlin.bertspeak.data.SettingsObserver


/**
 * Persistent application parameters are stored in a SQLite database. We check for
 * version when the database is first opened within this statis class.
 * The database is a Singleton (object) opened once and never closed.
 */
object DatabaseManager  {
    private val errorHandler :DbErrorHandler
    private val database:SQLiteDatabase
    private val observers: MutableMap<String, SettingsObserver>

    @Throws(SQLException::class)
    fun execSQL(sql: String)  {
        database.execSQL(sql)
    }

    /**
     * Trap and ignore any errors.
     * @param sql
     */
    fun execLenient(sql: String) {
        try {
            execSQL(sql)
        }
        catch (sqle: SQLException) {
            Log.i(CLSS, String.format("execLenient:%s: SQLException ignored (%s)",
                sql,sqle.localizedMessage))
        }
    }
    /**
     * Must be called when the manager is first created. If the database already exists on disk with the same name,
     * this method will have no effect. It is simply a guarantee that tables will exist.
     */
    fun initialize() {
        synchronized(this) {
            val SQL = StringBuilder()
            try {
                SQL.append("CREATE TABLE IF NOT EXISTS Settings (")
                SQL.append("  name TEXT PRIMARY KEY,")
                SQL.append("  value TEXT DEFAULT '',")
                SQL.append("  hint TEXT DEFAULT 'hint'")
                SQL.append(")")
                database.execSQL(SQL.toString())
                Log.i(CLSS, String.format("initialize: Created settings table in %s \n%s",
                    ConfigurationConstants.DB_NAME, SQL.toString()))
            }
            catch (sqle: SQLException) {
                Log.e(CLSS, String.format("initialize:aborted\n%s\nSQLException (%s)",
                    SQL,sqle.localizedMessage))
                return
            }
            // Use this statement when upgrading to version 2 of the database
            if( RE_INITIALIZE ) {
                var upgrade =
                    String.format("DELETE FROM Settings")
                execLenient(upgrade)
            }

            // Add initial settings - fail silently if they exist. The default values make sense
            // for development.
            var statement =
                String.format("INSERT INTO Settings(Name,Value,Hint) VALUES(\'%s\',\'%s\',\'%s\')",
                    ConfigurationConstants.BERT_HOST,
                    ConfigurationConstants.BERT_HOST_HINT,
                    ConfigurationConstants.BERT_HOST_HINT)
            execLenient(statement)
            statement =
                String.format("INSERT INTO Settings(Name,Value,Hint) VALUES(\'%s\',\'%s\',\'%s\')",
                    ConfigurationConstants.BERT_HOST_IP,
                    ConfigurationConstants.BERT_HOST_IP_HINT,
                    ConfigurationConstants.BERT_HOST_IP_HINT)
            execLenient(statement)
            statement =
                String.format("INSERT INTO Settings(Name,Value,Hint) VALUES(\'%s\',\'%s\',\'%s\')",
                    ConfigurationConstants.BERT_PORT,
                    ConfigurationConstants.BERT_PORT_HINT,
                    ConfigurationConstants.BERT_PORT_HINT)
            execLenient(statement)
            statement =
                String.format("INSERT INTO Settings(Name,Value,Hint) VALUES(\'%s\',\'%s\',\'%s\')",
                    ConfigurationConstants.BERT_VERSION,
                    ConfigurationConstants.BERT_VERSION_HINT,
                    ConfigurationConstants.BERT_VERSION_HINT)
            execLenient(statement)
            statement =
                String.format("INSERT INTO Settings(Name,Value,Hint) VALUES(\'%s\',\'%s\',\'%s\')",
                    ConfigurationConstants.BERT_VOLUME, ConfigurationConstants.BERT_VOLUME_HINT,
                    ConfigurationConstants.BERT_VOLUME_HINT)
            execLenient(statement)
        }
    }

    // ===================== Settings =============================
    /**
     * Read name/value pairs from the database.
     * Return an empty string if the field is not found
     */
    fun getSetting(name: String): String {
        synchronized(this) {
            var result: String = ""
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
            return result
        }
    }

    /**
     * Read name/value pairs from the database.
     */
    fun getSettings (): List<NameValue> {
        synchronized(this) {
            val list: MutableList<NameValue> = ArrayList()
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
            return list
        }
    }

    /**
     * Save a single setting to the database.
     * @param nv the subject name-value pair
     */
    fun updateSetting(nv: NameValue) {
        synchronized(this) {
            // Validate
            if( nv.name.equals(ConfigurationConstants.BERT_VOLUME)) {
                validateVolume(nv)
            }
            Log.i(CLSS, String.format("updateSettings: %s = %s (%s)", nv.name, nv.value, nv.hint))
            val SQL = "UPDATE Settings set value=?, hint=? WHERE name = ?"
            val bindArgs = arrayOfNulls<String>(3)
            bindArgs[0] = nv.value
            bindArgs[1] = nv.hint
            bindArgs[2] = nv.name
            database.execSQL(SQL, bindArgs)
            notifyObservers(nv)
        }
    }

    /**
     * Save settings to the database
     * @param items a list of name-value pairs
     */
    fun updateSettings(items: List<NameValue>) {
        synchronized(this) {
            val SQL = "UPDATE Settings set value=?, hint=? WHERE name = ?"
            val bindArgs = arrayOfNulls<String>(3)
            val count = items.size
            var index = 0
            while (index < count) {
                val nv = items[index]
                if( nv.name.equals(ConfigurationConstants.BERT_VOLUME)) {
                    validateVolume(nv)
                }
                bindArgs[0] = nv.value
                bindArgs[1] = nv.hint
                bindArgs[2] = nv.name
                database.execSQL(SQL, bindArgs)
                notifyObservers(nv)
                index++
            }
        }
    }

    /**
     * The volume is an integer 0-100 representing
     * a percentage of maximum.
     */
    private fun validateVolume(nv:NameValue) {
        var value = nv.value.toDouble()
        if( value.isNaN() ) value = 50.0
        else if( value >100.0 )   value = 100.0
        else if( value<0.0  )   value = 0.0
        nv.value = value.toInt().toString()
    }

    // If the database is open, close it to ensure correct writable flags
    private fun getDatabase(canWrite:Boolean) : SQLiteDatabase {
        var flags = SQLiteDatabase.CREATE_IF_NECESSARY
        flags = if( canWrite ) flags or SQLiteDatabase.OPEN_READWRITE
                else flags or SQLiteDatabase.OPEN_READONLY
        val db = SQLiteDatabase.openDatabase(
            ConfigurationConstants.DB_FILE_PATH, null,flags, errorHandler)

        db.setForeignKeyConstraintsEnabled(true)
        return db
    }

    /**
     * When a new observer is registered, update it
     *  with states of allmanagers.
     * @param observer
     */
    fun registerSettingsObserver(observer: SettingsObserver) {
        observers[observer.name] = observer
        val list: List<NameValue> = getSettings()
        observer.resetSettings(list)
    }

    fun unregisterSettingsObserver(observer: SettingsObserver) {
        observers.remove(observer.name)
    }

    /**
     * Notify observers of the facility-state change.
     */
    private fun notifyObservers(ddata: NameValue) {
        for (observer in observers.values) {
            observer.updateSetting(ddata)
        }
    }

    private const val CLSS = "DatabaseManager"
    private const val RE_INITIALIZE = false


    init {
        errorHandler = DbErrorHandler()
        database = getDatabase(true)   // Writable
        observers = mutableMapOf<String, SettingsObserver>()
    }

    /**
     * Check for the network in a separate thread.
     */
    private class DbErrorHandler: DatabaseErrorHandler{
        override fun onCorruption(dbObj: SQLiteDatabase) {
            Log.i("DbErrorHandler", String.format("onCorruption: %s",
                dbObj.path.toString()))
        }
    }
}
