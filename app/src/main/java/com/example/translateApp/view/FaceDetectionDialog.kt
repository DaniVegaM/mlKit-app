package com.example.translateApp.view

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.example.tempapplication.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * Dialog para detección de rostros usando ML Kit
 * Funcionalidad: Detección y análisis de rostros en imágenes
 */
class FaceDetectionDialog : DialogFragment() {
    private var imagePreview: ImageView? = null
    private var faceResultText: TextView? = null
    private var btnSelectImage: Button? = null
    private var btnUseCamera: Button? = null
    private var btnClose: Button? = null
    private var imageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                imageUri = data?.data
                imagePreview?.setImageURI(imageUri)
                imageUri?.let { detectFacesFromUri(it) }
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val bitmap = data?.extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    imagePreview?.setImageBitmap(bitmap)
                    detectFacesFromBitmap(bitmap)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_face_detection, container, false)
        imagePreview = view.findViewById(R.id.imagePreview)
        faceResultText = view.findViewById(R.id.faceResultText)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        btnUseCamera = view.findViewById(R.id.btnUseCamera)
        btnClose = view.findViewById(R.id.btnClose)

        btnSelectImage?.setOnClickListener { openGallery() }
        btnUseCamera?.setOnClickListener { openCamera() }
        btnClose?.setOnClickListener { dismiss() }
        imagePreview?.setOnClickListener { openGallery() }

        return view
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    private fun detectFacesFromUri(uri: Uri) {
        val context = requireContext()
        try {
            val image = InputImage.fromFilePath(context, uri)
            detectFaces(image)
        } catch (e: Exception) {
            Log.e("FaceDetection", "Error al procesar imagen desde URI", e)
            faceResultText?.text = "Error al procesar la imagen"
        }
    }

    private fun detectFacesFromBitmap(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        detectFaces(image)
    }

    private fun detectFaces(image: InputImage) {
        // Configuración del detector de rostros
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
                if (faces.isNotEmpty()) {
                    val result = StringBuilder()
                    result.append("Rostros detectados: ${faces.size}\n\n")
                    
                    faces.forEachIndexed { index, face ->
                        result.append("Rostro ${index + 1}:\n")
                        
                        // Información de sonrisa
                        face.smilingProbability?.let { smilingProb ->
                            val smilingPercentage = (smilingProb * 100).toInt()
                            result.append("• Sonriendo: ${smilingPercentage}%\n")
                        }
                        
                        // Información de ojos abiertos
                        face.leftEyeOpenProbability?.let { leftEyeProb ->
                            val leftEyePercentage = (leftEyeProb * 100).toInt()
                            result.append("• Ojo izquierdo abierto: ${leftEyePercentage}%\n")
                        }
                        
                        face.rightEyeOpenProbability?.let { rightEyeProb ->
                            val rightEyePercentage = (rightEyeProb * 100).toInt()
                            result.append("• Ojo derecho abierto: ${rightEyePercentage}%\n")
                        }
                        
                        // Ángulo de rotación
                        result.append("• Ángulo Y: ${face.headEulerAngleY.toInt()}°\n")
                        result.append("• Ángulo Z: ${face.headEulerAngleZ.toInt()}°\n")
                        
                        // Tamaño del rostro
                        val bounds = face.boundingBox
                        result.append("• Tamaño: ${bounds.width()} x ${bounds.height()}\n\n")
                    }
                    
                    faceResultText?.text = result.toString()
                } else {
                    faceResultText?.text = "No se detectaron rostros en la imagen"
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FaceDetection", "Error en la detección de rostros", exception)
                faceResultText?.text = "Error al analizar rostros: ${exception.message}"
            }
    }
}
