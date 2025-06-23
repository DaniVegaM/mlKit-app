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
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

/**
 * Dialog para identificación de puntos de referencia usando ML Kit
 * Funcionalidad: Identificación de monumentos y lugares famosos
 * Nota: Esta funcionalidad requiere conexión a internet y Firebase
 */
class LandmarkDetectionDialog : DialogFragment() {
    private var imagePreview: ImageView? = null
    private var landmarkResultText: TextView? = null
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
                imageUri?.let { detectLandmarksFromUri(it) }
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val bitmap = data?.extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    imagePreview?.setImageBitmap(bitmap)
                    detectLandmarksFromBitmap(bitmap)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_landmark_detection, container, false)
        imagePreview = view.findViewById(R.id.imagePreview)
        landmarkResultText = view.findViewById(R.id.landmarkResultText)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        btnUseCamera = view.findViewById(R.id.btnUseCamera)
        btnClose = view.findViewById(R.id.btnClose)

        btnSelectImage?.setOnClickListener { openGallery() }
        btnUseCamera?.setOnClickListener { openCamera() }
        btnClose?.setOnClickListener { dismiss() }
        imagePreview?.setOnClickListener { openGallery() }

        // Mostrar mensaje informativo inicial
        landmarkResultText?.text = "Selecciona una imagen de un monumento o lugar famoso para identificarlo.\n\nNota: Requiere conexión a internet."

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

    private fun detectLandmarksFromUri(uri: Uri) {
        val context = requireContext()
        try {
            val image = InputImage.fromFilePath(context, uri)
            detectLandmarks(image)
        } catch (e: Exception) {
            Log.e("LandmarkDetection", "Error al procesar imagen desde URI", e)
            landmarkResultText?.text = "Error al procesar la imagen"
        }
    }

    private fun detectLandmarksFromBitmap(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        detectLandmarks(image)
    }

    private fun detectLandmarks(image: InputImage) {
        landmarkResultText?.text = "Analizando imagen..."
        
        // Nota: Para una implementación completa de detección de landmarks,
        // necesitarías Firebase Cloud Vision API que requiere configuración adicional
        // Por ahora, usaremos Image Labeling que puede identificar algunos monumentos
        
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.3f)
            .build()

        val labeler = ImageLabeling.getClient(options)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                if (labels.isNotEmpty()) {
                    val result = StringBuilder()
                    result.append("Análisis de la imagen:\n\n")
                    
                    // Filtrar etiquetas que podrían ser landmarks
                    val landmarkKeywords = listOf(
                        "monument", "statue", "building", "architecture", "landmark",
                        "tower", "bridge", "cathedral", "church", "temple", "palace",
                        "castle", "museum", "fountain", "memorial", "monuments"
                    )
                    
                    val possibleLandmarks = labels.filter { label ->
                        landmarkKeywords.any { keyword ->
                            label.text.lowercase().contains(keyword)
                        }
                    }
                    
                    if (possibleLandmarks.isNotEmpty()) {
                        result.append("Posibles puntos de referencia detectados:\n\n")
                        possibleLandmarks.forEach { label ->
                            val confidence = (label.confidence * 100).toInt()
                            result.append("• ${label.text} (${confidence}% confianza)\n")
                        }
                    } else {
                        result.append("Elementos detectados:\n\n")
                        labels.take(5).forEach { label ->
                            val confidence = (label.confidence * 100).toInt()
                            result.append("• ${label.text} (${confidence}% confianza)\n")
                        }
                        result.append("\nNota: No se detectaron landmarks específicos. Para mejor precisión, configura Firebase Cloud Vision API.")
                    }
                    
                    landmarkResultText?.text = result.toString()
                } else {
                    landmarkResultText?.text = "No se detectaron elementos reconocibles en la imagen"
                }
            }
            .addOnFailureListener { exception ->
                Log.e("LandmarkDetection", "Error en la detección", exception)
                landmarkResultText?.text = "Error al analizar la imagen: ${exception.message}"
            }
    }
}
