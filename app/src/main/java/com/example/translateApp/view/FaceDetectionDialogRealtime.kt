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
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Dialog para detección de rostros usando ML Kit con cámara en tiempo real
 */
class FaceDetectionDialogRealtime : DialogFragment() {
    private var cameraPreview: PreviewView? = null
    private var faceResultText: TextView? = null
    private var btnSelectImage: Button? = null
    private var btnToggleCamera: Button? = null
    private var btnClose: Button? = null
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isAnalyzing = false

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val TAG = "FaceDetectionDialog"
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageUri = data?.data
                imageUri?.let { detectFacesFromUri(it) }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_face_detection_realtime, container, false)
        cameraPreview = view.findViewById(R.id.cameraPreview)
        faceResultText = view.findViewById(R.id.faceResultText)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        btnToggleCamera = view.findViewById(R.id.btnToggleCamera)
        btnClose = view.findViewById(R.id.btnClose)

        cameraExecutor = Executors.newSingleThreadExecutor()

        btnSelectImage?.setOnClickListener { openGallery() }
        btnToggleCamera?.setOnClickListener { toggleCameraAnalysis() }
        btnClose?.setOnClickListener { dismiss() }

        faceResultText?.text = "Presiona 'Iniciar Análisis' para comenzar la detección en tiempo real"

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
        faceResultText?.text = "Analizando rostros en tiempo real..."
        startCamera()
    }

    private fun stopAnalysis() {
        isAnalyzing = false
        btnToggleCamera?.text = "Iniciar Análisis"
        faceResultText?.text = "Análisis detenido. Presiona 'Iniciar Análisis' para continuar."
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
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer { faces ->
                        if (isAnalyzing) {
                            processFaceResults(faces)
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

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

    private fun processFaceResults(faces: List<Face>) {
        activity?.runOnUiThread {
            if (faces.isEmpty()) {
                faceResultText?.text = "No se detectaron rostros. Posiciona tu rostro frente a la cámara."
                return@runOnUiThread
            }

            val result = StringBuilder()
            result.append("Rostros detectados: ${faces.size}\n\n")

            for ((index, face) in faces.withIndex()) {
                result.append("Rostro ${index + 1}:\n")
                
                // Información de posición
                val bounds = face.boundingBox
                result.append("• Posición: (${bounds.left}, ${bounds.top}) - (${bounds.right}, ${bounds.bottom})\n")
                
                // Ángulos de rotación
                result.append("• Rotación Y: ${String.format("%.1f", face.headEulerAngleY)}°\n")
                result.append("• Rotación Z: ${String.format("%.1f", face.headEulerAngleZ)}°\n")
                
                // Probabilidad de sonrisa
                val smileProb = face.smilingProbability
                if (smileProb != null) {
                    result.append("• Probabilidad de sonrisa: ${String.format("%.1f", smileProb * 100)}%\n")
                }
                
                // Ojos abiertos
                val leftEyeOpenProb = face.leftEyeOpenProbability
                val rightEyeOpenProb = face.rightEyeOpenProbability
                
                if (leftEyeOpenProb != null) {
                    result.append("• Ojo izquierdo abierto: ${String.format("%.1f", leftEyeOpenProb * 100)}%\n")
                }
                
                if (rightEyeOpenProb != null) {
                    result.append("• Ojo derecho abierto: ${String.format("%.1f", rightEyeOpenProb * 100)}%\n")
                }
                
                result.append("\n")
            }

            faceResultText?.text = result.toString()

            // Guardar en historial
            val summary = "Detectados ${faces.size} rostro(s)"
            val historyItem = AnalysisHistoryItem(
                id = System.currentTimeMillis().toString(),
                type = "face_detection",
                inputText = "Análisis en tiempo real con cámara frontal",
                result = summary,
                confidence = 1.0f,
                timestamp = System.currentTimeMillis(),
                additionalData = mapOf("face_count" to faces.size.toString())
            )
            AnalysisHistory.addToHistory(requireContext(), historyItem)
        }
    }

    private fun detectFacesFromUri(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(requireContext(), uri)
            detectFaces(image)
        } catch (e: Exception) {
            Log.e(TAG, "Error creando imagen desde URI", e)
            faceResultText?.text = "Error procesando imagen: ${e.message}"
        }
    }

    private fun detectFaces(image: InputImage) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()

        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->
                processFaceResults(faces)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error detectando rostros", e)
                faceResultText?.text = "Error: ${e.message}"
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
                faceResultText?.text = "Permisos de cámara requeridos para análisis en tiempo real"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private class FaceAnalyzer(private val listener: (List<Face>) -> Unit) : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                val options = FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setMinFaceSize(0.15f)
                    .build()

                val detector = FaceDetection.getClient(options)
                
                detector.process(image)
                    .addOnSuccessListener { faces ->
                        listener(faces)
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
