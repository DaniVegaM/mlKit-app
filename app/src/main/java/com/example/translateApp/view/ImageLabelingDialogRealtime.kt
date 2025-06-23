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
import com.example.translateApp.utils.AnalysisHistory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Dialog para etiquetado de imágenes usando ML Kit con cámara en tiempo real
 */
class ImageLabelingRealtimeDialog : DialogFragment() {
    private var cameraPreview: PreviewView? = null
    private var labelResultText: TextView? = null
    private var btnSelectImage: Button? = null
    private var btnToggleCamera: Button? = null
    private var btnClose: Button? = null
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isAnalyzing = false

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageUri = data?.data
                imageUri?.let { labelImageFromUri(it) }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_image_labeling_realtime, container, false)
        cameraPreview = view.findViewById(R.id.cameraPreview)
        labelResultText = view.findViewById(R.id.labelResultText)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        btnToggleCamera = view.findViewById(R.id.btnToggleCamera)
        btnClose = view.findViewById(R.id.btnClose)

        cameraExecutor = Executors.newSingleThreadExecutor()

        btnSelectImage?.setOnClickListener { openGallery() }
        btnToggleCamera?.setOnClickListener { toggleCameraAnalysis() }
        btnClose?.setOnClickListener { dismiss() }

        labelResultText?.text = "Presiona 'Iniciar Análisis' para comenzar el etiquetado en tiempo real"

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        return view
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            startCamera()
        }
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
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                        if (isAnalyzing) {
                            analyzeImage(imageProxy)
                        } else {
                            imageProxy.close()
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
                Log.e("ImageLabeling", "Error al iniciar cámara", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun toggleCameraAnalysis() {
        isAnalyzing = !isAnalyzing
        btnToggleCamera?.text = if (isAnalyzing) "⏹️ Detener" else "▶️ Analizar"
        
        if (!isAnalyzing) {
            labelResultText?.text = "Presiona 'Analizar' para comenzar el etiquetado en tiempo real"
        }
    }

    private fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            labelImage(image)
        }
        imageProxy.close()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun labelImageFromUri(uri: Uri) {
        val context = requireContext()
        try {
            val image = InputImage.fromFilePath(context, uri)
            labelImage(image)
        } catch (e: Exception) {
            Log.e("ImageLabeling", "Error al procesar imagen desde URI", e)
            labelResultText?.text = "Error al procesar la imagen"
        }
    }

    private fun labelImage(image: InputImage) {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()

        val labeler = ImageLabeling.getClient(options)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                if (labels.isNotEmpty()) {
                    val result = StringBuilder()
                    result.append("🏷️ Objetos detectados:\n\n")
                    
                    labels.take(5).forEachIndexed { index, label ->
                        val confidence = (label.confidence * 100).toInt()
                        result.append("${index + 1}. ${label.text} (${confidence}%)\n")
                    }
                    
                    labelResultText?.text = result.toString()

                    // Guardar en historial
                    val labelTexts = labels.map { it.text }
                    val confidences = labels.map { it.confidence }
                    AnalysisHistory.addImageLabelingResult(requireContext(), labelTexts, confidences)
                    
                } else {
                    labelResultText?.text = "No se detectaron objetos en la imagen"
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ImageLabeling", "Error en el etiquetado", exception)
                labelResultText?.text = "Error al analizar la imagen"
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}
