package com.example.translateApp.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.translateApp.model.AnalysisHistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

object AnalysisHistory {
    
    private const val PREFS_NAME = "mlkit_analysis_history"
    private const val HISTORY_KEY = "analysis_history"
    private const val MAX_HISTORY_SIZE = 100
    
    private val gson = Gson()
    
    fun addToHistory(context: Context, item: AnalysisHistoryItem) {
        val prefs = getPreferences(context)
        val currentHistory = getHistory(context).toMutableList()
        
        currentHistory.add(0, item.copy(id = UUID.randomUUID().toString()))
        
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        val json = gson.toJson(currentHistory)
        prefs.edit().putString(HISTORY_KEY, json).apply()
    }
    
    fun getHistory(context: Context): List<AnalysisHistoryItem> {
        val prefs = getPreferences(context)
        val json = prefs.getString(HISTORY_KEY, null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<AnalysisHistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun clearHistory(context: Context) {
        val prefs = getPreferences(context)
        prefs.edit().remove(HISTORY_KEY).apply()
    }
    
    fun removeFromHistory(context: Context, itemId: String) {
        val prefs = getPreferences(context)
        val currentHistory = getHistory(context).toMutableList()
        
        currentHistory.removeAll { it.id == itemId }
        
        val json = gson.toJson(currentHistory)
        prefs.edit().putString(HISTORY_KEY, json).apply()
    }
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun addTextRecognitionResult(context: Context, inputImage: String, recognizedText: String) {
        val item = AnalysisHistoryItem(
            type = "text_recognition",
            inputText = "Imagen analizada",
            result = recognizedText,
            additionalData = mapOf("source" to "image")
        )
        addToHistory(context, item)
    }
    
    fun addTranslationResult(context: Context, sourceText: String, translatedText: String, sourceLang: String, targetLang: String) {
        val item = AnalysisHistoryItem(
            type = "translation",
            inputText = sourceText,
            result = translatedText,
            additionalData = mapOf(
                "source_language" to sourceLang,
                "target_language" to targetLang
            )
        )
        addToHistory(context, item)
    }
    
    fun addBarcodeResult(context: Context, barcodeData: String, format: String) {
        val item = AnalysisHistoryItem(
            type = "barcode_scanning",
            inputText = "Código escaneado",
            result = barcodeData,
            additionalData = mapOf("format" to format)
        )
        addToHistory(context, item)
    }
    
    fun addImageLabelingResult(context: Context, labels: List<String>, confidences: List<Float>) {
        val result = labels.zip(confidences).joinToString("\n") { (label, confidence) ->
            "$label (${(confidence * 100).toInt()}%)"
        }
        
        val item = AnalysisHistoryItem(
            type = "image_labeling",
            inputText = "Imagen analizada",
            result = result,
            confidence = confidences.maxOrNull() ?: 0f
        )
        addToHistory(context, item)
    }
    
    fun addFaceDetectionResult(context: Context, faceCount: Int, details: String) {
        val item = AnalysisHistoryItem(
            type = "face_detection",
            inputText = "Imagen analizada",
            result = "Rostros detectados: $faceCount\n$details",
            additionalData = mapOf("face_count" to faceCount.toString())
        )
        addToHistory(context, item)
    }
}
