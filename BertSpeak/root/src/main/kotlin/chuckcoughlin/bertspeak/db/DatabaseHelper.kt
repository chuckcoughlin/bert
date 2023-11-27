/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.db

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import chuckcoughlin.bertspeak.common.BertConstants

class DatabaseHelper(context:Context):
		SQLiteOpenHelper (context,BertConstants.DB_NAME,null,BertConstants.DB_VERSION) {

	override fun onCreate(db: SQLiteDatabase?) {
		if( db!=null ) {
			try {
				if(db.isOpen) {

					//db.close()
				}
				Log.i(CLSS, String.format("onCreate: database path = %s", db.path))
				DatabaseManager.initialize(db)
			}
			catch(sqle: SQLiteDatabaseLockedException) {
				Log.e(CLSS, String.format("onCreate: Database locked exception %s",
					sqle.localizedMessage))
			}
			catch(sqle: SQLException) {
				Log.e(CLSS, String.format("onCreate: Exception opening database %s",
				sqle.localizedMessage))
			}
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
