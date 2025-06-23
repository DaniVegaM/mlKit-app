package com.example.translateApp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Modelo de datos para los elementos del historial de análisis
 */
@Parcelize
data class AnalysisHistoryItem(
    val id: String = "",
    val type: String = "", // text_recognition, translation, barcode_scanning, etc.
    val inputText: String = "",
    val result: String = "",
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val additionalData: Map<String, String> = emptyMap()
) : Parcelable
