#!/bin/bash

# Script para generar PDFs desde archivos HTML usando macOS
# Desarrollado para ML Translate App
# Autores: Daniel Vega Miranda & Abraham Reyes Cuevas

echo "🚀 Generando PDFs para ML Translate App..."
echo "👥 Desarrollado por: Daniel Vega Miranda & Abraham Reyes Cuevas"
echo ""

# Verificar que los archivos HTML existan
if [ ! -f "Manual_de_Usuario_ML_Translate_App.html" ]; then
    echo "❌ Error: No se encontró Manual_de_Usuario_ML_Translate_App.html"
    exit 1
fi

if [ ! -f "Documentacion_Tecnica_ML_Translate_App.html" ]; then
    echo "❌ Error: No se encontró Documentacion_Tecnica_ML_Translate_App.html"
    exit 1
fi

# Función para convertir HTML a PDF usando wkhtmltopdf si está disponible
convert_with_wkhtmltopdf() {
    local html_file="$1"
    local pdf_file="$2"
    
    if command -v wkhtmltopdf &> /dev/null; then
        echo "🔄 Convirtiendo $html_file usando wkhtmltopdf..."
        wkhtmltopdf --page-size A4 --margin-top 0.75in --margin-right 0.75in --margin-bottom 0.75in --margin-left 0.75in "$html_file" "$pdf_file"
        return $?
    else
        return 1
    fi
}

# Función para convertir usando herramientas de macOS
convert_with_macos() {
    local html_file="$1"
    local pdf_file="$2"
    
    echo "🔄 Convirtiendo $html_file usando herramientas de macOS..."
    
    # Intentar con textutil primero
    if textutil -convert html -format html "$html_file" -output "/tmp/temp_$html_file" 2>/dev/null; then
        # Usar Safari via AppleScript para imprimir a PDF
        osascript <<EOF
tell application "Safari"
    activate
    open (POSIX file "$(pwd)/$html_file")
    delay 3
    
    tell application "System Events"
        tell process "Safari"
            keystroke "p" using command down
            delay 2
            click button "PDF" of group 1 of group 1 of sheet 1 of window 1
            delay 1
            click menu item "Save as PDF..." of menu 1 of button "PDF" of group 1 of group 1 of sheet 1 of window 1
            delay 2
            keystroke "$pdf_file"
            delay 1
            keystroke return
            delay 2
        end tell
    end tell
    
    close front window
end tell
EOF
        return $?
    else
        return 1
    fi
}

# Función de respaldo usando Python y weasyprint si está disponible
convert_with_python() {
    local html_file="$1"
    local pdf_file="$2"
    
    python3 -c "
try:
    import weasyprint
    weasyprint.HTML(filename='$html_file').write_pdf('$pdf_file')
    print('✅ Convertido exitosamente con weasyprint')
except ImportError:
    print('❌ weasyprint no disponible')
    exit(1)
except Exception as e:
    print(f'❌ Error: {e}')
    exit(1)
"
}

# Función para crear PDF básico desde el contenido Markdown
create_basic_pdf() {
    local md_file="$1"
    local pdf_name="$2"
    
    echo "📄 Creando PDF básico para $md_file..."
    
    # Crear un archivo temporal con formato simple
    cat > "/tmp/$pdf_name.txt" << EOF
ML TRANSLATE APP - DOCUMENTACIÓN
===============================
Desarrollado por: Daniel Vega Miranda & Abraham Reyes Cuevas
Fecha: Junio 2025
Versión: 2.0 - Edición Universitaria

$(cat "$md_file" | sed 's/^#\+\s*//' | sed 's/\*\*\(.*\)\**/\1/g' | sed 's/\*\(.*\)\*/\1/g')

EOF
    
    # Intentar convertir con textutil
    if textutil -convert pdf "/tmp/$pdf_name.txt" -output "$pdf_name.pdf" 2>/dev/null; then
        echo "✅ PDF básico creado: $pdf_name.pdf"
        rm "/tmp/$pdf_name.txt"
        return 0
    else
        rm "/tmp/$pdf_name.txt"
        return 1
    fi
}

# Convertir Manual de Usuario
echo "📖 Procesando Manual de Usuario..."
if ! convert_with_wkhtmltopdf "Manual_de_Usuario_ML_Translate_App.html" "Manual_de_Usuario_ML_Translate_App.pdf"; then
    if ! convert_with_python "Manual_de_Usuario_ML_Translate_App.html" "Manual_de_Usuario_ML_Translate_App.pdf"; then
        if ! create_basic_pdf "Manual_de_Usuario_ML_Translate_App.md" "Manual_de_Usuario_ML_Translate_App"; then
            echo "❌ Error creando PDF del Manual de Usuario"
        fi
    fi
fi

# Convertir Documentación Técnica
echo "🔧 Procesando Documentación Técnica..."
if ! convert_with_wkhtmltopdf "Documentacion_Tecnica_ML_Translate_App.html" "Documentacion_Tecnica_ML_Translate_App.pdf"; then
    if ! convert_with_python "Documentacion_Tecnica_ML_Translate_App.html" "Documentacion_Tecnica_ML_Translate_App.pdf"; then
        if ! create_basic_pdf "Documentacion_Tecnica_ML_Translate_App.md" "Documentacion_Tecnica_ML_Translate_App"; then
            echo "❌ Error creando PDF de la Documentación Técnica"
        fi
    fi
fi

# Verificar resultados
echo ""
echo "📋 Resultados:"
if [ -f "Manual_de_Usuario_ML_Translate_App.pdf" ]; then
    echo "✅ Manual de Usuario PDF generado exitosamente"
    ls -lh "Manual_de_Usuario_ML_Translate_App.pdf"
else
    echo "❌ Error generando Manual de Usuario PDF"
fi

if [ -f "Documentacion_Tecnica_ML_Translate_App.pdf" ]; then
    echo "✅ Documentación Técnica PDF generada exitosamente"
    ls -lh "Documentacion_Tecnica_ML_Translate_App.pdf"
else
    echo "❌ Error generando Documentación Técnica PDF"
fi

echo ""
echo "🎉 Proceso completado!"
echo "👥 Autores: Daniel Vega Miranda & Abraham Reyes Cuevas"
