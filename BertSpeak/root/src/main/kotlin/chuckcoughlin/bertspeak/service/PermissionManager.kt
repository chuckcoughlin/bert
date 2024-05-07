/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import chuckcoughlin.bertspeak.R


/**
 * Check runtime permissions, then ask user to enable, if necessary
 */
class PermissionManager(act:FragmentActivity)  {
	private val permissions:Array<String>
    private val activity: FragmentActivity = act

	fun askForPermissions() {
        for(permission in permissions) {
            if(activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                Log.i(CLSS,String.format("Permission %s GRANTED",permission))
            }
            else if(ActivityCompat.shouldShowRequestPermissionRationale(activity,permission)){
                val builder = AlertDialog.Builder(activity)
                builder.setMessage("This app requires runtime permissions to work")
                builder.setTitle("Permission Required")
                builder.setCancelable(false)
                builder.setPositiveButton(R.string.buttonOK,ButtonCallback(this,permission,ACTION_GRANT_PERMISSION) )
                builder.setNegativeButton(R.string.buttonCancel) { dialog,_ -> dialog.dismiss()}
                builder.show()
            }
            else {
                ActivityCompat.requestPermissions(activity, arrayOf(permission),PERMISSION_REQ_CODE)
            }
        }
	}

    /*
    @Override
    fun onRequestPermissionsResult(requestCode:Int,permissions:String[],results: Int[]) {
        activity.onRequestPermissionsResult(requestCode,permissions,results)
        if(requestCode==PERMISSION_REQ_CODE) {
            if(results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED) {
                Log.i(CLSS,"Permission granted")
            }
            else if(!ActivityCompat.shouldShowRequestPermissionRationale(act,permission)) {
                val builder = AlertDialog.Builder(activity)
                builder.setMessage("This app requires permissions to work")
                builder.setTitle("Permission Required")
                builder.setCancelable(false)
                builder.setPositiveButton("Settings",DialogInterface.onClickListener() {
                    @Override
                    fun onCLick(dialog:DialogInterface,which:Int) {
                        intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        uri = Uri.fromParts("package",getPackageName(),null)
                        intent.setData(uri)
                        startActivity(intent)
                        dialog.dismiss()
                    }
                })
                builder.setNegativeButton("Cancel",(dialog,which) -> dialog.dismiss()))
                builder.show()
            }

        }
    }
    */


    /* ============================ On Click Listener ================================== */
    class ButtonCallback(pm:PermissionManager,permission:String,what:Int) : DialogInterface.OnClickListener {
        val mgr = pm
        val key = what
        val perm = permission
        override fun onClick(dialog: DialogInterface?, which: Int) {
            if( key==mgr.ACTION_GRANT_PERMISSION) {
                ActivityCompat.requestPermissions(mgr.activity,arrayOf(perm),mgr.PERMISSION_REQ_CODE)
            }
            if( dialog!=null) dialog.dismiss()
        }
    }


    private val CLSS = "PermissionManager"
    private val ACTION_GRANT_PERMISSION = 1
    private val PERMISSION_REQ_CODE = 10201

	init {
		permissions = arrayOf<String>(
            Manifest.permission.INTERNET,
			Manifest.permission.RECORD_AUDIO
		)
	}
}
