package chuckcoughlin.bertspeak.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import chuckcoughlin.bertspeak.common.BertConstants

class DatabaseHelper(context:Context):
		SQLiteOpenHelper (context,BertConstants.DB_NAME,null,BertConstants.DB_VERSION) {

	override fun onCreate() {

	}

	override fun onCreate(db: SQLiteDatabase?) {
		if( db!=null ) {
			Log.i(CLSS, String.format("onCreate: database path = %s", db.path))
			DatabaseManager.initialize(db)
		}
		else {
			Log.e(CLSS, String.format("onCreate: database is null"))
		}
	}

	override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
		if( oldVersion!=newVersion) {
			Log.i(CLSS, String.format("onUpgrade: converting %d -> %d.",oldVersion,newVersion))
		}
	}

	companion object {
		private const val CLSS = "DatabaseHelper"
	}
}
