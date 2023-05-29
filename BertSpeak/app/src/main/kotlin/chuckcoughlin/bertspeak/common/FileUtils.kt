/*
 * Copyright (C) 2023 Chuck Coughlin
 * MIT License
 */
package chuckcoughlin.bertspeak.common

import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Utilities for file manipulation
 */
object FileUtils {
    private const val CLSS = "FileUtils"
    /**
     * Creates intermediate directories, if needed, and then
     * the final file.
     *
     * @param pathString the complete path, an absolute path
     * @return the File object for the path
     */
    fun ensureFileExists(pathString: String): File {
        var file:File? = null
        Log.i(CLSS,"ensureFileExists: "+pathString)
        var mark = 1 // Keep track of position in path
        try {
            while(mark>0) {
                mark = pathString.indexOf(File.separatorChar,mark,false)
                if( mark<0 ) break
                val dir = File(pathString.substring(0,mark))
                Log.i(CLSS,"checking: "+dir.absolutePath.toString())
                if( !dir.exists()) dir.mkdir()
                mark++
            }
            // Finally create the file
            file = File(pathString)
            Log.i(CLSS,"checking the file: "+file.absolutePath.toString())
            if(!file.exists()) file.createNewFile()
        }
        catch (uoe: UnsupportedOperationException) {
            Log.e(CLSS, String.format("ensureFileExists: %s: UnsupportedOperation (%s)",
                pathString,uoe.localizedMessage))
        }
        catch (fae: FileAlreadyExistsException) {
            Log.e(CLSS, String.format("ensureFileExists: %s FileAlreadyExists (%s)",
                pathString,fae.localizedMessage))
        }
        catch (ioe: IOException) {
            Log.e(CLSS, String.format("ensureFileExists: %s IOException (%s)",
                pathString,ioe.localizedMessage))
        }
        catch (se: SecurityException) {
            Log.e(CLSS, String.format("ensureFileExists: %s SecurityException (%s)",
                pathString,se.localizedMessage))
        }
        return file!!
    }
}