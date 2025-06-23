# Manual de Usuario
## ML Translate App - Versión Universitaria con ML Kit

**Desarrollado por:**
- Daniel Vega Miranda
- Abraham Reyes Cuevas

**Fecha:** Junio 2025  
**Versión:** 2.0 - Edición Universitaria

---

## 📱 Introducción

ML Translate App es una aplicación Android avanzada que integra múltiples funcionalidades de Machine Learning (ML Kit) para análisis de imágenes, texto y detección de objetos en tiempo real. Esta versión universitaria incluye más de 5 funcionalidades diferentes de ML Kit con capacidades tanto offline como online.

## 🚀 Características Principales

### ✨ Funcionalidades ML Kit Integradas:

1. **📝 Reconocimiento de Texto (OCR)**
   - Extracción de texto de imágenes
   - Soporte para múltiples idiomas
   - Procesamiento offline

2. **🌐 Traducción de Texto**
   - Traducción entre múltiples idiomas
   - Detección automática de idioma
   - Funcionalidad offline después de descargar paquetes

3. **📱 Escáner de Códigos de Barras (Tiempo Real)**
   - QR Codes, códigos de barras UPC, EAN
   - Análisis en tiempo real con cámara
   - Múltiples formatos soportados

4. **🏷️ Etiquetado de Imágenes (Tiempo Real)**
   - Identificación automática de objetos
   - Análisis en tiempo real con cámara
   - Niveles de confianza para cada etiqueta

5. **👥 Detección de Rostros (Tiempo Real)**
   - Detección facial en tiempo real
   - Análisis de expresiones (sonrisa, ojos abiertos)
   - Información de posición y rotación

6. **🏛️ Detección de Monumentos (Tiempo Real)**
   - Identificación de lugares y monumentos
   - Análisis arquitectónico
   - Clasificación de edificios históricos

7. **📊 Historial de Análisis**
   - Registro completo de todos los análisis
   - Detalles y metadatos de cada operación
   - Visualización organizada por fecha

## 🎯 Cómo Usar la Aplicación

### 1. Pantalla Principal
- **Campo de texto central:** Para introducir texto a traducir
- **Botón "Detectar Idioma":** Identifica automáticamente el idioma del texto
- **Selector de idioma origen y destino:** Para configurar la traducción
- **Botón "Traducir":** Ejecuta la traducción
- **Botón ML Kit (⚡):** Accede a todas las funcionalidades avanzadas

### 2. Menú ML Kit Features

Al tocar el botón ML Kit (⚡), se abre un menú con todas las funcionalidades:

#### 📝 Reconocimiento de Texto
- Toca para abrir el selector de imagen
- Selecciona una imagen de la galería o toma una foto
- El texto extraído aparece automáticamente en el campo principal

#### 🌐 Traducción
- Toca para enfocar el campo de texto
- Introduce texto manualmente o usa OCR
- Configura idiomas y traduce

#### 📱 Escáner de Códigos de Barras
- Se abre la cámara en tiempo real
- **"Iniciar Análisis":** Comienza el escaneo automático
- **"Galería":** Selecciona imagen de galería
- **Formatos soportados:** QR, UPC, EAN, Code 128, Data Matrix, PDF417, etc.

#### 🏷️ Etiquetado de Imágenes
- Cámara en tiempo real para identificar objetos
- **"Iniciar Análisis":** Análisis automático continuo
- **"Galería":** Analizar imagen guardada
- Muestra etiquetas con porcentaje de confianza

#### 👥 Detección de Rostros
- Cámara frontal en tiempo real
- **"Iniciar Análisis":** Detección automática
- **Información mostrada:**
  - Número de rostros detectados
  - Posición y dimensiones
  - Probabilidad de sonrisa
  - Estado de ojos (abiertos/cerrados)
  - Ángulos de rotación

#### 🏛️ Detección de Monumentos
- Cámara trasera para análisis arquitectónico
- **"Iniciar Análisis":** Identificación automática
- **Características:**
  - Identificación de edificios históricos
  - Clasificación arquitectónica
  - Análisis de elementos estructurales

### 3. Historial de Análisis

- **Acceso:** Botón "Historial" en la pantalla principal
- **Visualización:** Lista cronológica de todos los análisis
- **Detalles:** Toca cualquier elemento para ver información completa
- **Información incluida:**
  - Tipo de análisis realizado
  - Resultado obtenido
  - Fecha y hora
  - Nivel de confianza
  - Metadatos adicionales

## 🎨 Interfaz de Usuario

### Controles de Cámara en Tiempo Real:
- **🟢 Iniciar Análisis:** Comienza el procesamiento automático
- **🔴 Detener Análisis:** Pausa el análisis en tiempo real
- **📁 Galería:** Selecciona imagen guardada
- **❌ Cerrar:** Regresa al menú principal

### Indicadores Visuales:
- **Vista previa de cámara:** Muestra la imagen en tiempo real
- **Panel de resultados:** Información del análisis actualizada continuamente
- **Niveles de confianza:** Porcentajes de precisión para cada detección

## 🔧 Configuración y Permisos

### Permisos Requeridos:
- **📷 Cámara:** Para análisis en tiempo real
- **📁 Almacenamiento:** Para acceso a galería
- **🌐 Internet:** Para descargar paquetes de idiomas (opcional)

### Configuración Offline:
- La mayoría de funcionalidades trabajan completamente offline
- Para traducción: descarga paquetes de idiomas la primera vez
- OCR y detección funcionan sin conexión

## 🚨 Solución de Problemas

### Problemas Comunes:

1. **Cámara no funciona:**
   - Verifica permisos de cámara en Configuración > Aplicaciones
   - Reinicia la aplicación

2. **Traducción no disponible:**
   - Asegúrate de tener conexión a internet para descargar paquetes
   - Los paquetes se descargan automáticamente la primera vez

3. **OCR no detecta texto:**
   - Asegúrate de que el texto sea legible
   - Mejora la iluminación
   - Acerca o aleja la cámara para mejor enfoque

4. **Detección lenta:**
   - Cierra otras aplicaciones para liberar memoria
   - En dispositivos antiguos, usa modo galería en lugar de tiempo real

## 📈 Rendimiento Óptimo

### Recomendaciones:
- **Iluminación:** Usa buena iluminación para mejor precisión
- **Estabilidad:** Mantén el dispositivo estable durante análisis
- **Distancia:** Posición óptima a 30-50cm del objeto
- **Memoria:** Cierra aplicaciones innecesarias para mejor rendimiento

## 🆘 Soporte y Contacto

Para soporte técnico o reportar problemas:
- **Desarrolladores:** Daniel Vega Miranda & Abraham Reyes Cuevas
- **Proyecto:** Práctica Universitaria - ML Kit Integration
- **Versión:** 2.0 - Edición Universitaria

---

*Este manual cubre todas las funcionalidades implementadas en la versión universitaria de ML Translate App. La aplicación cumple con los requisitos académicos de integración de múltiples tecnologías ML Kit con interfaz intuitiva y funcionalidades tanto offline como online.*
