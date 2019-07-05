package chuckcoughlin.bert.db;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import chuckcoughlin.bert.common.NameValue;
import chuckcoughlin.bert.common.BertConstants;


/**
 * Persistent application parameters are stored in a SQLite database. The SQLiteOpenHelper
 * checks for existence and version when the database is first opened. Beyond that the
 * checks are ignored. Create a separate instance of this class wherever needed.
 * The database is closed after each transaction.
 */
public class DatabaseManager extends SQLiteOpenHelper {
    private final static String CLSS = "DatabaseManager";
    private volatile Context context = null;

    /**
     * Constructor requires the activity context.
     * @param context main activity
     */
    public DatabaseManager(Context context) {
        super(context, BertConstants.DB_NAME, null, BertConstants.DB_VERSION);
        this.context = context.getApplicationContext();
    }


    /**
     * Called when the database connection is being configured.
     * Configure database settings for things like foreign key support, write-ahead logging, etc.
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Called when the database is created for the FIRST time.
     * If a database already exists on disk with the same DATABASE_NAME, this method will NOT be called.
     * @param sqLiteDatabase
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        StringBuilder SQL = new StringBuilder();
        SQL.append("CREATE TABLE IF NOT EXISTS Settings (");
        SQL.append("  name TEXT PRIMARY KEY,");
        SQL.append("  value TEXT DEFAULT '',");
        SQL.append("  hint TEXT DEFAULT 'hint'");
        SQL.append(")");
        sqLiteDatabase.execSQL(SQL.toString());


        // Add initial rows - fail silently if they exist. Use default for value.
        String statement = "INSERT INTO Settings(Name,Hint) VALUES('"+BertConstants.BERT_SERVER+"','"+BertConstants.BERT_SERVER_HINT+"')";
        execLenient(sqLiteDatabase,statement);
        statement = "INSERT INTO Settings(Name,Hint) VALUES('"+BertConstants.BERT_PORT+"','"+BertConstants.BERT_PORT_HINT+"')";
        execLenient(sqLiteDatabase,statement);
        statement = "INSERT INTO Settings(Name,Hint) VALUES('"+BertConstants.BERT_PAIRED_DEVICE+"','"+BertConstants.BERT_PAIRED_DEVICE_HINT+"')";
        execLenient(sqLiteDatabase,statement);
        statement = "INSERT INTO Settings(Name,Hint) VALUES('"+BertConstants.BERT_SERVICE_UUID+"','"+BertConstants.BERT_SERVICE_UUID_HINT+"')";
        execLenient(sqLiteDatabase,statement);

        Log.i(CLSS,String.format("onCreate: Created %s at %s",BertConstants.DB_NAME,context.getDatabasePath(BertConstants.DB_NAME)));
    }

    /**
     * Trap and log any errors.
     * @param sql
     */
    public void execSQL(String sql) {
        SQLiteDatabase database = this.getWritableDatabase();
        try {
            database.execSQL(sql);
        }
        catch(SQLException sqle) {
            Log.e(CLSS,String.format("execSQL:%s; SQLException ignored (%s)",sql,sqle.getLocalizedMessage()));
        }
    }

    /**
     * Trap and ignore any errors.
     * @param sqLiteDatabase
     * @param sql
     */
    public void execLenient(SQLiteDatabase sqLiteDatabase,String sql) {
        try {
            sqLiteDatabase.execSQL(sql);
        }
        catch(SQLException sqle) {
            Log.i(CLSS,String.format("SQLException ignored (%s)",sqle.getLocalizedMessage()));
        }
    }

    /**
     * Alter an existing database to account for changes as time goes on. This is called if the
     * database is accessed with a newer version than exists on disk.
     * @param sqLiteDatabase the database
     * @param oldVersion version number of the existing installation
     * @param newVersion current version number
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if( oldVersion==BertConstants.DB_VERSION) return; // Already at latest version
        try {
            onCreate(sqLiteDatabase);
        }
        catch(SQLException sqle) {
            Log.e(CLSS, String.format("onUpgrade: SQLError: %s", sqle.getLocalizedMessage()));
        }
    }

    // ================================================ Settings =============================
    /**
     * Read name/value pairs from the database.
     */
    public String getSetting(String name) {
        String result = null;
        SQLiteDatabase database = this.getReadableDatabase();
        String[] args = new String[1];   // Use for PreparedStatement
        args[0] = name;
        String SQL = "SELECT value FROM Settings WHERE Name=?";
        Cursor cursor = database.rawQuery(SQL,args);
        cursor.moveToFirst();

        if( !cursor.isAfterLast() ) {
            result = cursor.getString(0);
            Log.i(CLSS,String.format("getSetting: %s = %s",name,result));
        }
        cursor.close();
        database.close();
        return result;
    }
    /**
     * Read name/value pairs from the database.
     */
    public List<NameValue> getSettings() {
        List<NameValue> list = new ArrayList<>();
        SQLiteDatabase database = this.getReadableDatabase();
        String[] args = new String[0];   // Use for PreparedStatement
        String SQL = "SELECT name,value,hint FROM Settings ORDER BY Name";
        Cursor cursor = database.rawQuery(SQL,args);
        cursor.moveToFirst();

        while( !cursor.isAfterLast() ) {
            NameValue nv = new NameValue();
            nv.setName(cursor.getString(0));
            nv.setValue(cursor.getString(1));
            nv.setHint(cursor.getString(2));
            Log.i(CLSS,String.format("getSettings: %s = %s (%s)",nv.getName(),nv.getValue(),nv.getHint()));
            list.add(nv);
            cursor.moveToNext();
        }
        cursor.close();
        database.close();
        return list;
    }
    /**
     * Save a single setting to the database.
     * @param nv the subject name-value pair
     */
    public void updateSetting(NameValue nv) {
        SQLiteDatabase database = this.getWritableDatabase();
        String SQL = "UPDATE Settings set value=?, hint=? WHERE name = ?";
        String[] bindArgs = new String[3];

        bindArgs[0] = nv.getValue();
        bindArgs[1] = nv.getHint();
        bindArgs[2] = nv.getName();
        database.execSQL(SQL,bindArgs);
        database.close();
    }

    /**
     * Save settings to the database
     * @param items a list of name-value pairs
     */
    public void updateSettings(List<NameValue> items) {
        SQLiteDatabase database = this.getWritableDatabase();
        String SQL = "UPDATE Settings set value=?, hint=? WHERE name = ?";
        String[] bindArgs = new String[3];
        int count = items.size();
        int index = 0;
        while( index<count) {
            NameValue nv = items.get(index);
            bindArgs[0] = nv.getValue();
            bindArgs[1] = nv.getHint();
            bindArgs[2] = nv.getName();
            database.execSQL(SQL,bindArgs);
            index++;
        }
        database.close();
    }
}
