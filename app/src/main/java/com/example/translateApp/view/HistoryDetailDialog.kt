package com.example.translateApp.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.tempapplication.R
import com.example.translateApp.model.AnalysisHistoryItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Diálogo para mostrar los detalles completos de un elemento del historial
 */
class HistoryDetailDialog : DialogFragment() {
    
    private var analysisItem: AnalysisHistoryItem? = null
    
    companion object {
        private const val ARG_ANALYSIS_ITEM = "analysis_item"
        
        fun newInstance(item: AnalysisHistoryItem): HistoryDetailDialog {
            val fragment = HistoryDetailDialog()
            val args = Bundle()
            args.putParcelable(ARG_ANALYSIS_ITEM, item)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            analysisItem = it.getParcelable(ARG_ANALYSIS_ITEM)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_history_detail, container, false)
        
        setupViews(view)
        
        return view
    }
    
    private fun setupViews(view: View) {
        val typeText: TextView = view.findViewById(R.id.detailAnalysisType)
        val inputText: TextView = view.findViewById(R.id.detailInputText)
        val resultText: TextView = view.findViewById(R.id.detailResultText)
        val dateText: TextView = view.findViewById(R.id.detailAnalysisDate)
        val confidenceText: TextView = view.findViewById(R.id.detailConfidence)
        val additionalDataText: TextView = view.findViewById(R.id.detailAdditionalData)
        val closeButton: Button = view.findViewById(R.id.btnCloseDetail)
        
        analysisItem?.let { item ->
            typeText.text = getTypeDisplayName(item.type)
            inputText.text = if (item.inputText.isNotEmpty()) item.inputText else "N/A"
            resultText.text = item.result
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            dateText.text = "Fecha: ${dateFormat.format(Date(item.timestamp))}"
            
            if (item.confidence > 0f) {
                confidenceText.text = "Confianza: ${(item.confidence * 100).toInt()}%"
                confidenceText.visibility = View.VISIBLE
            } else {
                confidenceText.visibility = View.GONE
            }
            
            if (item.additionalData.isNotEmpty()) {
                val additionalDataString = item.additionalData.entries.joinToString("\n") { (key, value) ->
                    "${formatKey(key)}: $value"
                }
                additionalDataText.text = "Información adicional:\n$additionalDataString"
                additionalDataText.visibility = View.VISIBLE
            } else {
                additionalDataText.visibility = View.GONE
            }
        }
        
        closeButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun getTypeDisplayName(type: String): String {
        return when (type) {
            "text_recognition" -> "📄 Reconocimiento de Texto"
            "translation" -> "🌐 Traducción"
            "barcode_scanning" -> "📊 Código de Barras"
            "image_labeling" -> "🏷️ Etiquetado de Imagen"
            "face_detection" -> "😊 Detección de Rostros"
            "landmark_detection" -> "🏛️ Detección de Monumentos"
            else -> "🔍 Análisis ML Kit"
        }
    }
    
    private fun formatKey(key: String): String {
        return when (key) {
            "source" -> "Fuente"
            "source_language" -> "Idioma origen"
            "target_language" -> "Idioma destino"
            "format" -> "Formato"
            "face_count" -> "Cantidad de rostros"
            else -> key.replaceFirstChar { it.uppercase() }
        }
    }
}
