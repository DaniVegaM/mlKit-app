# Documentación Técnica
## ML Translate App - Arquitectura y Implementación

**Desarrollado por:**
- Daniel Vega Miranda
- Abraham Reyes Cuevas

**Fecha:** Junio 2025  
**Versión:** 2.0 - Edición Universitaria

---

## 🏗️ Arquitectura General

### Stack Tecnológico:
- **Lenguaje:** Kotlin
- **Framework:** Android SDK (API 21+)
- **ML Framework:** Google ML Kit
- **Cámara:** CameraX
- **UI:** Material Design Components
- **Persistencia:** SharedPreferences + Gson
- **Arquitectura:** MVVM Pattern con Fragment-based UI

### Estructura del Proyecto:
```
app/src/main/java/com/example/translateApp/
├── model/
│   └── AnalysisHistoryItem.kt
├── utils/
│   ├── AnalysisHistory.kt
│   └── CameraUtils.kt
├── view/
│   ├── TranslateActivity.kt (Actividad Principal)
│   ├── HistoryActivity.kt
│   ├── MLKitFeaturesDialog.kt
│   ├── ImageLabelingDialog.kt
│   ├── ImageLabelingDialogRealtime.kt
│   ├── BarcodeScannerDialogRealtime.kt
│   ├── FaceDetectionDialogRealtime.kt
│   ├── LandmarkDetectionDialogRealtime.kt
│   └── HistoryDetailDialog.kt
└── res/
    ├── layout/ (Layouts XML)
    └── values/ (Strings, Colors, Themes)
```

## 🧩 Componentes Principales

### 1. TranslateActivity.kt
**Responsabilidad:** Actividad principal que maneja traducción básica y coordinación general.

**Características clave:**
- Detección automática de idioma
- Traducción offline/online
- OCR integrado
- Navegación a funcionalidades ML Kit

**Métodos públicos expuestos:**
```kotlin
fun openImageSelector() // Para OCR desde menú ML Kit
fun focusOnTextInput()  // Para enfocar campo de texto
```

### 2. MLKitFeaturesDialog.kt
**Responsabilidad:** Menú central para acceso a todas las funcionalidades ML Kit.

**Funcionalidades:**
- Navegación a diálogos especializados
- Coordinación con actividad principal
- Interfaz unificada para acceso a ML Kit

### 3. Diálogos de Tiempo Real

#### BarcodeScannerRealtimeDialog.kt
**Tecnología:** ML Kit Barcode Scanning + CameraX
**Formatos soportados:**
- QR_CODE, AZTEC, EAN_13, EAN_8
- UPC_A, UPC_E, CODE_39, CODE_93
- CODE_128, DATA_MATRIX, PDF417, ITF, CODABAR

**Implementación:**
```kotlin
private class BarcodeAnalyzer(private val listener: (List<Barcode>) -> Unit) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient(options)
            scanner.process(image)
                .addOnSuccessListener { barcodes -> listener(barcodes) }
                .addOnCompleteListener { imageProxy.close() }
        }
    }
}
```

#### FaceDetectionDialogRealtime.kt
**Tecnología:** ML Kit Face Detection + CameraX
**Características detectadas:**
- Posición y dimensiones del rostro
- Probabilidad de sonrisa
- Estado de ojos (abierto/cerrado)
- Ángulos de rotación (Euler Y, Z)
- Seguimiento facial

**Configuración del detector:**
```kotlin
val options = FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
    .setMinFaceSize(0.15f)
    .build()
```

#### ImageLabelingRealtimeDialog.kt
**Tecnología:** ML Kit Image Labeling + CameraX
**Capacidades:**
- Identificación de objetos en tiempo real
- Niveles de confianza configurables
- Etiquetado multicategoría

#### LandmarkDetectionDialogRealtime.kt
**Tecnología:** ML Kit Image Labeling (filtrado por palabras clave) + CameraX
**Enfoque especializado:**
- Filtrado por palabras clave arquitectónicas
- Identificación de monumentos y edificios
- Análisis de elementos históricos

**Palabras clave filtradas:**
```kotlin
private val LANDMARK_KEYWORDS = setOf(
    "building", "architecture", "monument", "statue", "church", 
    "cathedral", "temple", "palace", "castle", "tower", "bridge",
    "landmark", "museum", "historical", "ancient", "art"
)
```

## 🎥 Implementación CameraX

### Configuración Base:
```kotlin
private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
    
    cameraProviderFuture.addListener({
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(cameraPreview?.surfaceProvider)
        }
        
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, AnalyzerImplementation { results ->
                    if (isAnalyzing) processResults(results)
                })
            }
        
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
    }, ContextCompat.getMainExecutor(requireContext()))
}
```

### Estrategia de Análisis:
- **Backpressure:** `STRATEGY_KEEP_ONLY_LATEST` para rendimiento óptimo
- **Threading:** `SingleThreadExecutor` para procesamiento ML
- **UI Updates:** `runOnUiThread` para actualización segura de UI

## 💾 Sistema de Persistencia

### Modelo de Datos:
```kotlin
@Parcelize
data class AnalysisHistoryItem(
    val id: String = "",
    val type: String = "", // text_recognition, barcode_scanning, etc.
    val inputText: String = "",
    val result: String = "",
    val confidence: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val additionalData: Map<String, String> = emptyMap()
) : Parcelable
```

### Gestión de Historial:
```kotlin
object AnalysisHistory {
    private const val MAX_HISTORY_SIZE = 100
    
    fun addToHistory(context: Context, item: AnalysisHistoryItem) {
        val prefs = getPreferences(context)
        val currentHistory = getHistory(context).toMutableList()
        currentHistory.add(0, item.copy(id = UUID.randomUUID().toString()))
        
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        val json = gson.toJson(currentHistory)
        prefs.edit().putString(HISTORY_KEY, json).apply()
    }
}
```

## 🔒 Gestión de Permisos

### Permisos Implementados:
```kotlin
companion object {
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private const val PERMISSION_REQUEST_CODE = 1001
}

private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
}
```

### Flujo de Permisos:
1. Verificación al inicializar diálogo
2. Solicitud automática si no están concedidos
3. Fallback a modo galería si se deniegan
4. Mensaje informativo al usuario

## 🏗️ Patrones de Diseño Implementados

### 1. Observer Pattern
- Callback listeners para análisis ML Kit
- UI updates mediante `runOnUiThread`

### 2. Strategy Pattern
- Diferentes analizadores para cada tipo de ML Kit
- Interfaz común `ImageAnalysis.Analyzer`

### 3. Factory Pattern
- Creación de diálogos especializados
- Configuración de detectores ML Kit

### 4. Singleton Pattern
- `AnalysisHistory` para gestión centralizada
- `CameraUtils` para utilidades compartidas

## 📊 Optimizaciones de Rendimiento

### ML Kit:
- **Confidence Threshold:** Configurado por funcionalidad
- **Performance Mode:** FAST para tiempo real, ACCURATE para análisis estático
- **Image Resolution:** Optimizada automáticamente por CameraX

### CameraX:
- **BackpressureStrategy:** Solo procesa frame más reciente
- **Threading:** Ejecutor dedicado para ML processing
- **Memory Management:** Cierre automático de `ImageProxy`

### UI:
- **Fragment-based:** Carga modular de funcionalidades
- **RecyclerView:** Para historial con ViewHolder pattern
- **Lazy Loading:** Inicialización bajo demanda

## 🔧 Configuración Build

### Dependencies Clave:
```gradle
// CameraX para cámara en tiempo real
implementation "androidx.camera:camera-core:1.3.1"
implementation "androidx.camera:camera-camera2:1.3.1"
implementation "androidx.camera:camera-lifecycle:1.3.1"
implementation "androidx.camera:camera-view:1.3.1"

// ML Kit modules
implementation 'com.google.mlkit:translate:17.0.2'
implementation 'com.google.mlkit:language-id:17.0.5'
implementation 'com.google.mlkit:text-recognition:16.0.0'
implementation 'com.google.mlkit:barcode-scanning:17.2.0'
implementation 'com.google.mlkit:image-labeling:17.0.7'
implementation 'com.google.mlkit:face-detection:16.1.5'

// Persistencia y utilidades
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'androidx.cardview:cardview:1.0.0'
```

### Configuración ProGuard:
- Keep classes ML Kit
- Preserve modelo annotations
- Mantener interfaces CameraX

## 🧪 Testing y Debugging

### Estrategias de Debug:
- Logging detallado con tags específicos
- Try-catch comprehensivo para ML operations
- Validación de permisos en runtime
- Error handling para fallos de cámara

### Métricas de Performance:
- Tiempo de inicialización de cámara
- Latencia de análisis ML Kit
- Uso de memoria durante operaciones
- Framerate de análisis en tiempo real

## 🚀 Deployment

### Requisitos Mínimos:
- Android API 21+ (Android 5.0)
- Camera2 API support
- Mínimo 2GB RAM para rendimiento óptimo
- 50MB espacio libre para paquetes ML Kit

### Optimizaciones APK:
- Modularización ML Kit (descarga bajo demanda)
- Compresión de recursos
- Eliminación de código no utilizado
- Vector drawables para iconografía

## 🔮 Arquitectura Extensible

### Puntos de Extensión:
1. **Nuevos Detectores ML Kit:** Implementar `ImageAnalysis.Analyzer`
2. **Nuevos Formatos de Export:** Extender `AnalysisHistory`
3. **UI Personalizada:** Fragment modular system
4. **Nuevos Idiomas:** Configuración en `strings.xml`

### Consideraciones Futuras:
- Integración con Firebase ML Kit
- Soporte para modelos custom TensorFlow Lite
- Análisis de video en tiempo real
- Sincronización cloud del historial

---

## 📋 Resumen Técnico

**Funcionalidades ML Kit Implementadas:** 6+
- ✅ Text Recognition (OCR)
- ✅ Language Translation
- ✅ Barcode Scanning (Real-time)
- ✅ Image Labeling (Real-time)
- ✅ Face Detection (Real-time)
- ✅ Landmark Detection (Real-time)

**Características Técnicas:**
- ✅ Cámara en tiempo real (CameraX)
- ✅ Análisis offline/online
- ✅ Historial persistente
- ✅ Interfaz intuitiva
- ✅ Gestión de permisos
- ✅ Optimización de rendimiento
- ✅ Arquitectura modular y extensible

**Cumplimiento Académico:**
- ✅ Más de 3 funcionalidades ML Kit diferentes
- ✅ Combinación e integración de tecnologías
- ✅ Interfaz intuitiva y moderna
- ✅ Procesamiento offline y online
- ✅ Visualización clara de resultados
- ✅ Gestión completa de historial
- ✅ Adaptación para plataforma especializada (Android)

*Esta documentación técnica detalla la implementación completa de ML Translate App versión universitaria, desarrollada como proyecto académico integrando múltiples tecnologías de Machine Learning con arquitectura Android moderna.*
