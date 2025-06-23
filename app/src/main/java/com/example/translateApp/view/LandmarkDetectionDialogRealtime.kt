package com.example.translateApp.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.tempapplication.R
import com.example.translateApp.model.AnalysisHistoryItem
import com.example.translateApp.utils.AnalysisHistory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Dialog para detección de monumentos y lugares usando ML Kit con cámara en tiempo real
 * Utiliza etiquetado de imágenes general enfocado en arquitectura, monumentos y lugares
 */
class LandmarkDetectionDialogRealtime : DialogFragment() {
    private var cameraPreview: PreviewView? = null
    private var landmarkResultText: TextView? = null
    private var btnSelectImage: Button? = null
    private var btnToggleCamera: Button? = null
    private var btnClose: Button? = null
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isAnalyzing = false

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val TAG = "LandmarkDetectionDialog"
        
        // Palabras clave relacionadas con monumentos y lugares
        private val LANDMARK_KEYWORDS = setOf(
            "building", "architecture", "monument", "statue", "church", "cathedral", 
            "temple", "palace", "castle", "tower", "bridge", "landmark", "museum",
            "historical", "ancient", "art", "sculpture", "fountain", "plaza",
            "square", "park", "garden", "city", "urban", "street", "facade",
            "dome", "column", "arch", "gate", "wall", "stone", "marble",
            "bronze", "memorial", "historic", "heritage", "cultural"
        )
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageUri = data?.data
                imageUri?.let { detectLandmarksFromUri(it) }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_landmark_detection_realtime, container, false)
        cameraPreview = view.findViewById(R.id.cameraPreview)
        landmarkResultText = view.findViewById(R.id.landmarkResultText)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        btnToggleCamera = view.findViewById(R.id.btnToggleCamera)
        btnClose = view.findViewById(R.id.btnClose)

        cameraExecutor = Executors.newSingleThreadExecutor()

        btnSelectImage?.setOnClickListener { openGallery() }
        btnToggleCamera?.setOnClickListener { toggleCameraAnalysis() }
        btnClose?.setOnClickListener { dismiss() }

        landmarkResultText?.text = "Presiona 'Iniciar Análisis' para comenzar la detección en tiempo real"

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        return view
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun toggleCameraAnalysis() {
        if (isAnalyzing) {
            stopAnalysis()
        } else {
            startAnalysis()
        }
    }

    private fun startAnalysis() {
        isAnalyzing = true
        btnToggleCamera?.text = "Detener Análisis"
        landmarkResultText?.text = "Analizando monumentos y lugares en tiempo real..."
        startCamera()
    }

    private fun stopAnalysis() {
        isAnalyzing = false
        btnToggleCamera?.text = "Iniciar Análisis"
        landmarkResultText?.text = "Análisis detenido. Presiona 'Iniciar Análisis' para continuar."
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreview?.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LandmarkAnalyzer { labels ->
                        if (isAnalyzing) {
                            processLandmarkResults(labels)
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Error iniciando cámara", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processLandmarkResults(labels: List<ImageLabel>) {
        activity?.runOnUiThread {
            if (labels.isEmpty()) {
                landmarkResultText?.text = "Enfoca monumentos, edificios o lugares de interés..."
                return@runOnUiThread
            }

            // Filtrar etiquetas relacionadas con monumentos y lugares
            val landmarkLabels = labels.filter { label ->
                val text = label.text.lowercase()
                LANDMARK_KEYWORDS.any { keyword -> text.contains(keyword) }
            }.sortedByDescending { it.confidence }

            val result = StringBuilder()
            
            if (landmarkLabels.isNotEmpty()) {
                result.append("🏛️ Monumentos/Lugares detectados:\n\n")
                
                for ((index, label) in landmarkLabels.withIndex()) {
                    val confidence = (label.confidence * 100).toInt()
                    result.append("${index + 1}. ${label.text}")
                    result.append(" (${confidence}% confianza)\n")
                }
                
                result.append("\n")
            }

            // Mostrar también otras etiquetas relevantes
            val otherLabels = labels.filter { label ->
                val text = label.text.lowercase()
                !LANDMARK_KEYWORDS.any { keyword -> text.contains(keyword) }
            }.take(5)

            if (otherLabels.isNotEmpty()) {
                result.append("🏷️ Otras etiquetas detectadas:\n\n")
                
                for ((index, label) in otherLabels.withIndex()) {
                    val confidence = (label.confidence * 100).toInt()
                    result.append("${index + 1}. ${label.text}")
                    result.append(" (${confidence}% confianza)\n")
                }
            }

            if (result.isEmpty()) {
                landmarkResultText?.text = "No se detectaron monumentos o lugares específicos. Enfoca edificios, monumentos o lugares de interés."
            } else {
                landmarkResultText?.text = result.toString()

                // Guardar en historial
                val mainLandmark = landmarkLabels.firstOrNull()?.text ?: "Lugar detectado"
                val historyItem = AnalysisHistoryItem(
                    id = System.currentTimeMillis().toString(),
                    type = "landmark_detection",
                    inputText = "Análisis en tiempo real - ${landmarkLabels.size} lugares identificados",
                    result = mainLandmark,
                    confidence = landmarkLabels.firstOrNull()?.confidence ?: 0.0f,
                    timestamp = System.currentTimeMillis(),
                    additionalData = mapOf("landmark_count" to landmarkLabels.size.toString())
                )
                AnalysisHistory.addToHistory(requireContext(), historyItem)
            }
        }
    }

    private fun detectLandmarksFromUri(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(requireContext(), uri)
            detectLandmarks(image)
        } catch (e: Exception) {
            Log.e(TAG, "Error creando imagen desde URI", e)
            landmarkResultText?.text = "Error procesando imagen: ${e.message}"
        }
    }

    private fun detectLandmarks(image: InputImage) {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()

        val labeler = ImageLabeling.getClient(options)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                processLandmarkResults(labels)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error detectando monumentos", e)
                landmarkResultText?.text = "Error: ${e.message}"
            }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        requestPermissions(REQUIRED_PERMISSIONS, 1001)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == 1001) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                landmarkResultText?.text = "Permisos de cámara requeridos para análisis en tiempo real"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private class LandmarkAnalyzer(private val listener: (List<ImageLabel>) -> Unit) : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                val options = ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.4f)
                    .build()

                val labeler = ImageLabeling.getClient(options)
                
                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        listener(labels)
                    }
                    .addOnFailureListener { 
                        // Error handling
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
}
