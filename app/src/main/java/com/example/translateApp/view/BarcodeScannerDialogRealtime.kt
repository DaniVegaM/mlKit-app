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
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Dialog para escáner de códigos de barras usando ML Kit con cámara en tiempo real
 */
class BarcodeScannerRealtimeDialog : DialogFragment() {
    private var cameraPreview: PreviewView? = null
    private var barcodeResultText: TextView? = null
    private var btnSelectImage: Button? = null
    private var btnToggleCamera: Button? = null
    private var btnClose: Button? = null
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isAnalyzing = false

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val TAG = "BarcodeScannerDialog"
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageUri = data?.data
                imageUri?.let { scanBarcodeFromUri(it) }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_barcode_scanner_realtime, container, false)
        cameraPreview = view.findViewById(R.id.cameraPreview)
        barcodeResultText = view.findViewById(R.id.barcodeResultText)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        btnToggleCamera = view.findViewById(R.id.btnToggleCamera)
        btnClose = view.findViewById(R.id.btnClose)

        cameraExecutor = Executors.newSingleThreadExecutor()

        btnSelectImage?.setOnClickListener { openGallery() }
        btnToggleCamera?.setOnClickListener { toggleCameraAnalysis() }
        btnClose?.setOnClickListener { dismiss() }

        barcodeResultText?.text = "Presiona 'Iniciar Análisis' para comenzar el escaneo en tiempo real"

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
        barcodeResultText?.text = "Analizando códigos de barras en tiempo real..."
        startCamera()
    }

    private fun stopAnalysis() {
        isAnalyzing = false
        btnToggleCamera?.text = "Iniciar Análisis"
        barcodeResultText?.text = "Análisis detenido. Presiona 'Iniciar Análisis' para continuar."
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
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodes ->
                        if (isAnalyzing) {
                            processBarcodeResults(barcodes)
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

    private fun processBarcodeResults(barcodes: List<Barcode>) {
        activity?.runOnUiThread {
            if (barcodes.isEmpty()) {
                barcodeResultText?.text = "Enfoca un código de barras para escanearlo..."
                return@runOnUiThread
            }

            val result = StringBuilder()
            result.append("Códigos detectados:\n\n")

            for ((index, barcode) in barcodes.withIndex()) {
                result.append("${index + 1}. ")
                
                when (barcode.valueType) {
                    Barcode.TYPE_TEXT -> {
                        result.append("Texto: ${barcode.displayValue}")
                    }
                    Barcode.TYPE_URL -> {
                        result.append("URL: ${barcode.url?.url}")
                    }
                    Barcode.TYPE_EMAIL -> {
                        result.append("Email: ${barcode.email?.address}")
                    }
                    Barcode.TYPE_PHONE -> {
                        result.append("Teléfono: ${barcode.phone?.number}")
                    }
                    Barcode.TYPE_PRODUCT -> {
                        result.append("Producto: ${barcode.displayValue}")
                    }
                    Barcode.TYPE_WIFI -> {
                        result.append("WiFi: ${barcode.wifi?.ssid}")
                    }
                    else -> {
                        result.append("Contenido: ${barcode.displayValue}")
                    }
                }
                
                result.append(" (${getBarcodeFormat(barcode.format)})")
                result.append("\n\n")
            }

            barcodeResultText?.text = result.toString()

            // Guardar en historial solo el primer código detectado
            if (barcodes.isNotEmpty()) {
                val firstBarcode = barcodes[0]
                val barcodeData = "${getBarcodeFormat(firstBarcode.format)}: ${firstBarcode.displayValue}"
                val historyItem = AnalysisHistoryItem(
                    id = System.currentTimeMillis().toString(),
                    type = "barcode_scanning",
                    inputText = "Escaneado en tiempo real",
                    result = barcodeData,
                    confidence = 1.0f,
                    timestamp = System.currentTimeMillis(),
                    additionalData = mapOf("format" to getBarcodeFormat(firstBarcode.format))
                )
                AnalysisHistory.addToHistory(requireContext(), historyItem)
            }
        }
    }

    private fun getBarcodeFormat(format: Int): String {
        return when (format) {
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_CODE_93 -> "CODE_93"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            else -> "DESCONOCIDO"
        }
    }

    private fun scanBarcodeFromUri(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(requireContext(), uri)
            scanBarcode(image)
        } catch (e: Exception) {
            Log.e(TAG, "Error creando imagen desde URI", e)
            barcodeResultText?.text = "Error procesando imagen: ${e.message}"
        }
    }

    private fun scanBarcode(image: InputImage) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_CODABAR
            )
            .build()

        val scanner = BarcodeScanning.getClient(options)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                processBarcodeResults(barcodes)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error escaneando código de barras", e)
                barcodeResultText?.text = "Error: ${e.message}"
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
                barcodeResultText?.text = "Permisos de cámara requeridos para análisis en tiempo real"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private class BarcodeAnalyzer(private val listener: (List<Barcode>) -> Unit) : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_AZTEC,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_93,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_PDF417,
                        Barcode.FORMAT_ITF,
                        Barcode.FORMAT_CODABAR
                    )
                    .build()

                val scanner = BarcodeScanning.getClient(options)
                
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        listener(barcodes)
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
