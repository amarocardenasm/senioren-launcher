package com.seniorenlauncher.util

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Linterna como interruptor: un toque enciende, otro apaga.
 * No necesita permiso de camara (setTorchMode no lo pide).
 */
object Torch {
    private val _isOn = MutableStateFlow(false)
    val isOn: StateFlow<Boolean> = _isOn

    private var cameraId: String? = null
    private var registered = false

    private fun manager(context: Context) =
        context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun init(context: Context) {
        if (registered) return
        val cm = manager(context)
        cameraId = try {
            cm.cameraIdList.firstOrNull { id ->
                val c = cm.getCameraCharacteristics(id)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.e("Torch", "No se pudo leer la camara", e)
            null
        }
        // Mantiene el estado real aunque la linterna se apague desde otro lado
        cm.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) _isOn.value = enabled
            }
        }, Handler(Looper.getMainLooper()))
        registered = true
    }

    fun toggle(context: Context) {
        init(context)
        val id = cameraId
        if (id == null) {
            Toast.makeText(context, "Este teléfono no tiene linterna", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val newState = !_isOn.value
            manager(context).setTorchMode(id, newState)
            _isOn.value = newState
        } catch (e: Exception) {
            Log.e("Torch", "No se pudo cambiar la linterna", e)
            Toast.makeText(context, "No se pudo usar la linterna", Toast.LENGTH_SHORT).show()
        }
    }
}
