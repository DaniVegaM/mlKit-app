package com.example.translateApp.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Utilidades para manejo de cámara y permisos en diálogos ML Kit
 */
object CameraUtils {
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    const val PERMISSION_REQUEST_CODE = 1001
    
    /**
     * Verifica si todos los permisos de cámara están concedidos
     */
    fun allPermissionsGranted(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Formatea el porcentaje de confianza para mostrar
     */
    fun formatConfidence(confidence: Float): String {
        return String.format("%.1f", confidence * 100)
    }
    
    /**
     * Formatea ángulos de rotación para mostrar
     */
    fun formatAngle(angle: Float): String {
        return String.format("%.1f", angle)
    }
}
