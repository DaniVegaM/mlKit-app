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
import com.example.translateApp.model.AnalysisHistoryItem
import com.example.translateApp.utils.AnalysisHistory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

/**
 * Dialog para etiquetado de imágenes usando ML Kit
 * Funcionalidad: Identificación automática de objetos y conceptos en imágenes
 */
class ImageLabelingDialog : DialogFragment() {
    private var imagePreview: ImageView? = null
    private var labelResultText: TextView? = null
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
                imageUri?.let { labelImageFromUri(it) }
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val bitmap = data?.extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    imagePreview?.setImageBitmap(bitmap)
                    labelImageFromBitmap(bitmap)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_image_labeling, container, false)
        imagePreview = view.findViewById(R.id.imagePreview)
        labelResultText = view.findViewById(R.id.labelResultText)
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

    private fun labelImageFromBitmap(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        labelImage(image)
    }

    private fun labelImage(image: InputImage) {
        // Configuración del etiquetador de imágenes
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f) // Umbral de confianza del 50%
            .build()

        val labeler = ImageLabeling.getClient(options)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                if (labels.isNotEmpty()) {
                    val result = StringBuilder()
                    result.append("Objetos detectados:\n\n")
                    
                    labels.forEachIndexed { index, label ->
                        val confidence = (label.confidence * 100).toInt()
                        result.append("${index + 1}. ${label.text} (${confidence}% confianza)\n")
                    }
                    
                    labelResultText?.text = result.toString()
                } else {
                    labelResultText?.text = "No se detectaron objetos en la imagen"
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ImageLabeling", "Error en el etiquetado", exception)
                labelResultText?.text = "Error al analizar la imagen: ${exception.message}"
            }
    }
}
