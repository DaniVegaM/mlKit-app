package com.example.translateApp.view

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.example.tempapplication.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeScannerDialog : DialogFragment() {
    private var barcodePreview: ImageView? = null
    private var barcodeResultText: TextView? = null
    private var btnSelectImage: Button? = null
    private var btnUseCamera: Button? = null
    private var btnClose: Button? = null
    private var imageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                imageUri = data?.data
                barcodePreview?.setImageURI(imageUri)
                imageUri?.let { scanBarcodeFromUri(it) }
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val bitmap = data?.extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    barcodePreview?.setImageBitmap(bitmap)
                    scanBarcodeFromBitmap(bitmap)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_barcode_scanner, container, false)
        barcodePreview = view.findViewById(R.id.barcodePreview)
        barcodeResultText = view.findViewById(R.id.barcodeResultText)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        btnUseCamera = view.findViewById(R.id.btnUseCamera)
        btnClose = view.findViewById(R.id.btnClose)

        btnSelectImage?.setOnClickListener { openGallery() }
        btnUseCamera?.setOnClickListener { openCamera() }
        btnClose?.setOnClickListener { dismiss() }
        barcodePreview?.setOnClickListener { openGallery() }

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

    private fun scanBarcodeFromUri(uri: Uri) {
        val context = requireContext()
        val image = InputImage.fromFilePath(context, uri)
        scanBarcode(image)
    }

    private fun scanBarcodeFromBitmap(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        scanBarcode(image)
    }

    private fun scanBarcode(image: InputImage) {
        val context = requireContext()
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS
            )
            .build()
        val scanner: BarcodeScanner = BarcodeScanning.getClient(options)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val result = barcodes.joinToString("\n") { it.rawValue ?: "" }
                    barcodeResultText?.text = "Código detectado:\n$result"
                } else {
                    barcodeResultText?.text = "No se detectó ningún código de barras"
                }
            }
            .addOnFailureListener {
                barcodeResultText?.text = "Error al procesar la imagen"
            }
    }
}
