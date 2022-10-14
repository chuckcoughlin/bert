package chuckcoughlin.bertspeak.db

import android.content.*
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import chuckcoughlin.bertspeak.common.*

/**
 * Persistent application parameters are stored in a SQLite database. The SQLiteOpenHelper
 * checks for existence and version when the database is first opened. Beyond that the
 * checks are ignored. Create a separate instance of this class wherever needed.
 * The database is closed after each transaction.
 */
class DatabaseManager(ctx: Context) :
    SQLiteOpenHelper(ctx, BertConstants.DB_NAME, null, BertConstants.DB_VERSION) {
    private var context: Context = ctx

    /**
     * Called when the database connection is being configured.
     * Configure database settings for things like foreign key support, write-ahead logging, etc.
     */
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    /**
     * Called when the database is created for the FIRST time.
     * If a database already exists on disk with the same DATABASE_NAME, this method will NOT be called.
     * @param sqLiteDatabase
     */
    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        val SQL = StringBuilder()
        SQL.append("CREATE TABLE IF NOT EXISTS Settings (")
        SQL.append("  name TEXT PRIMARY KEY,")
        SQL.append("  value TEXT DEFAULT '',")
        SQL.append("  hint TEXT DEFAULT 'hint'")
        SQL.append(")")
        sqLiteDatabase.execSQL(SQL.toString())


        // Add initial rows - fail silently if they exist. Use default for value.
        var statement =
            "INSERT INTO Settings(Name,Hint) VALUES('" + BertConstants.BERT_SERVER + "','" + BertConstants.BERT_SERVER_HINT + "')"
        execLenient(sqLiteDatabase, statement)
        statement =
            "INSERT INTO Settings(Name,Hint) VALUES('" + BertConstants.BERT_PORT + "','" + BertConstants.BERT_PORT_HINT + "')"
        execLenient(sqLiteDatabase, statement)
        statement =
            "INSERT INTO Settings(Name,Hint) VALUES('" + BertConstants.BERT_PAIRED_DEVICE + "','" + BertConstants.BERT_PAIRED_DEVICE_HINT + "')"
        execLenient(sqLiteDatabase, statement)
        statement =
            "INSERT INTO Settings(Name,Hint) VALUES('" + BertConstants.BERT_SERVICE_UUID + "','" + BertConstants.BERT_SERVICE_UUID_HINT + "')"
        execLenient(sqLiteDatabase, statement)
        statement =
            "INSERT INTO Settings(Name,Hint) VALUES('" + BertConstants.BERT_SIMULATED_CONNECTION + "','" + BertConstants.BERT_SIMULATED_CONNECTION_HINT + "')"
        execLenient(sqLiteDatabase, statement)
        Log.i(
            CLSS,
            String.format(
                "onCreate: Created %s at %s",
                BertConstants.DB_NAME,
                context!!.getDatabasePath(BertConstants.DB_NAME)
            )
        )
    }

    /**
     * Trap and log any errors.
     * @param sql
     */
    fun execSQL(sql: String?) {
        val database = this.writableDatabase
        try {
            database.execSQL(sql)
        } catch (sqle: SQLException) {
            Log.e(
                CLSS,
                String.format("execSQL:%s; SQLException ignored (%s)", sql, sqle.localizedMessage)
            )
        }
    }

    /**
     * Trap and ignore any errors.
     * @param sqLiteDatabase
     * @param sql
     */
    fun execLenient(sqLiteDatabase: SQLiteDatabase, sql: String?) {
        try {
            sqLiteDatabase.execSQL(sql)
        } catch (sqle: SQLException) {
            Log.i(CLSS, String.format("SQLException ignored (%s)", sqle.localizedMessage))
        }
    }

    /**
     * Alter an existing database to account for changes as time goes on. This is called if the
     * database is accessed with a newer version than exists on disk.
     * @param sqLiteDatabase the database
     * @param oldVersion version number of the existing installation
     * @param newVersion current version number
     */
    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == BertConstants.DB_VERSION) return  // Already at latest version
        try {
            onCreate(sqLiteDatabase)
        } catch (sqle: SQLException) {
            Log.e(CLSS, String.format("onUpgrade: SQLError: %s", sqle.localizedMessage))
        }
    }
    // ================================================ Settings =============================
    /**
     * Read name/value pairs from the database.
     */
    fun getSetting(name: String): String? {
        var result: String? = null
        val database = this.readableDatabase
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
    }// Use for PreparedStatement

    /**
     * Read name/value pairs from the database.
     */
    val settings: List<NameValue>
        get() {
            val list: MutableList<NameValue> = ArrayList()
            val database = this.readableDatabase
            val args = arrayOfNulls<String>(0) // Use for PreparedStatement
            val SQL = "SELECT name,value,hint FROM Settings ORDER BY Name"
            val cursor = database.rawQuery(SQL, args)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val nv = NameValue(cursor.getString(0),cursor.getString(1),cursor.getString(2))
                Log.i(CLSS, String.format("getSettings: %s = %s (%s)", nv.name, nv.value, nv.hint))
                list.add(nv)
                cursor.moveToNext()
            }
            cursor.close()
            database.close()
            return list
        }

    /**
     * Save a single setting to the database.
     * @param nv the subject name-value pair
     */
    fun updateSetting(nv: NameValue?) {
        val database = this.writableDatabase
        val SQL = "UPDATE Settings set value=?, hint=? WHERE name = ?"
        val bindArgs = arrayOfNulls<String>(3)
        bindArgs[0] = nv!!.value
        bindArgs[1] = nv.hint
        bindArgs[2] = nv.name
        database.execSQL(SQL, bindArgs)
        database.close()
    }

    /**
     * Save settings to the database
     * @param items a list of name-value pairs
     */
    fun updateSettings(items: List<NameValue>) {
        val database = this.writableDatabase
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

    companion object {
        private const val CLSS = "DatabaseManager"
    }

    /**
     * Constructor requires the activity context.
     * @param context main activity
     */
    init {
        this.context = context.applicationContext
    }
}
