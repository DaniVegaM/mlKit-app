package com.example.translateApp.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tempapplication.R
import com.example.translateApp.model.AnalysisHistoryItem
import com.example.translateApp.utils.AnalysisHistory
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad para mostrar el historial de análisis ML Kit
 * Cumple con el requisito de "Guardado y gestión del historial de análisis"
 */
class HistoryActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var emptyView: TextView
    private lateinit var clearHistoryBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        
        initializeViews()
        setupRecyclerView()
        loadHistory()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.historyRecyclerView)
        emptyView = findViewById(R.id.emptyHistoryView)
        clearHistoryBtn = findViewById(R.id.clearHistoryBtn)
        
        clearHistoryBtn.setOnClickListener {
            clearHistory()
        }
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(mutableListOf()) { item ->
            // Callback para cuando se hace click en un item
            showItemDetails(item)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadHistory() {
        val historyItems = AnalysisHistory.getHistory(this)
        adapter.updateItems(historyItems)
        
        if (historyItems.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            clearHistoryBtn.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            clearHistoryBtn.visibility = View.VISIBLE
        }
    }

    private fun clearHistory() {
        AnalysisHistory.clearHistory(this)
        loadHistory()
    }

    private fun showItemDetails(item: AnalysisHistoryItem) {
        // Aquí podrías mostrar un diálogo con los detalles completos
        val detailDialog = HistoryDetailDialog.newInstance(item)
        detailDialog.show(supportFragmentManager, "HistoryDetail")
    }
}

/**
 * Adapter para el RecyclerView del historial
 */
class HistoryAdapter(
    private val items: MutableList<AnalysisHistoryItem>,
    private val onItemClick: (AnalysisHistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val typeText: TextView = view.findViewById(R.id.analysisType)
        val resultText: TextView = view.findViewById(R.id.analysisResult)
        val dateText: TextView = view.findViewById(R.id.analysisDate)
        val container: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.typeText.text = getTypeDisplayName(item.type)
        holder.resultText.text = if (item.result.length > 100) {
            item.result.substring(0, 97) + "..."
        } else {
            item.result
        }
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.dateText.text = dateFormat.format(Date(item.timestamp))
        
        holder.container.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<AnalysisHistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
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
}
