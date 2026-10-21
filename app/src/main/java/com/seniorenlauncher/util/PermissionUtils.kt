package com.seniorenlauncher.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {
    
    fun hasSosPermissions(context: Context): Boolean {
        val basePermissions = listOf(Manifest.permission.CALL_PHONE)
        
        return basePermissions.all { 
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
        }
    }

    fun getRequiredSosPermissions(): List<String> {
        return listOf(Manifest.permission.CALL_PHONE)
    }
}
