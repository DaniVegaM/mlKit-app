#!/usr/bin/env python3
"""
Generador de PDFs simple para ML Translate App
Autores: Daniel Vega Miranda & Abraham Reyes Cuevas
"""

import os
from datetime import datetime

def create_simple_pdf_content(md_file, title):
    """Convierte contenido Markdown a formato de texto plano estructurado"""
    try:
        with open(md_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Crear contenido estructurado
        output = f"""
ML TRANSLATE APP - {title.upper()}
{'=' * 60}

Desarrollado por: Daniel Vega Miranda & Abraham Reyes Cuevas
Fecha: {datetime.now().strftime('%d de %B de %Y')}
Versión: 2.0 - Edición Universitaria

{'=' * 60}

"""
        
        # Procesar líneas del Markdown
        lines = content.split('\n')
        in_code_block = False
        
        for line in lines:
            line = line.strip()
            
            if line.startswith('```'):
                in_code_block = not in_code_block
                if in_code_block:
                    output += "\n--- CÓDIGO ---\n"
                else:
                    output += "--- FIN CÓDIGO ---\n\n"
                continue
            
            if in_code_block:
                output += "    " + line + "\n"
                continue
            
            # Procesar headers
            if line.startswith('# '):
                output += "\n" + "=" * 60 + "\n"
                output += line[2:].upper() + "\n"
                output += "=" * 60 + "\n\n"
            elif line.startswith('## '):
                output += "\n" + "-" * 40 + "\n"
                output += line[3:] + "\n"
                output += "-" * 40 + "\n\n"
            elif line.startswith('### '):
                output += "\n• " + line[4:].upper() + "\n"
                output += "  " + "-" * len(line[4:]) + "\n\n"
            elif line.startswith('#### '):
                output += "\n  ◦ " + line[5:] + "\n\n"
            elif line.startswith('- '):
                output += "  • " + line[2:] + "\n"
            elif line.startswith('**') and line.endswith('**'):
                output += "\n[IMPORTANTE] " + line[2:-2] + "\n\n"
            elif line.startswith('*') and line.endswith('*'):
                output += line[1:-1] + "\n"
            elif line == '---':
                output += "\n" + "~" * 60 + "\n\n"
            elif line and not line.startswith('#'):
                output += line + "\n"
            elif not line:
                output += "\n"
        
        return output
        
    except Exception as e:
        return f"Error procesando {md_file}: {e}"

def main():
    print("🚀 Generando documentación PDF para ML Translate App")
    print("👥 Autores: Daniel Vega Miranda & Abraham Reyes Cuevas")
    print()
    
    # Archivos a procesar
    files = [
        ('Manual_de_Usuario_ML_Translate_App.md', 'Manual de Usuario'),
        ('Documentacion_Tecnica_ML_Translate_App.md', 'Documentación Técnica')
    ]
    
    for md_file, title in files:
        if os.path.exists(md_file):
            print(f"📄 Procesando: {title}")
            
            # Generar contenido de texto
            content = create_simple_pdf_content(md_file, title)
            
            # Crear archivo de texto
            txt_file = md_file.replace('.md', '.txt')
            with open(txt_file, 'w', encoding='utf-8') as f:
                f.write(content)
            
            print(f"✅ Archivo de texto generado: {txt_file}")
            
            # Intentar convertir a PDF usando textutil
            pdf_file = md_file.replace('.md', '.pdf')
            try:
                import subprocess
                result = subprocess.run(['textutil', '-convert', 'pdf', txt_file, '-output', pdf_file], 
                                      capture_output=True, text=True)
                if result.returncode == 0:
                    print(f"✅ PDF generado exitosamente: {pdf_file}")
                else:
                    print(f"⚠️  textutil falló, manteniendo archivo de texto: {txt_file}")
            except Exception as e:
                print(f"⚠️  Error con textutil: {e}")
                print(f"📄 Archivo de texto disponible: {txt_file}")
        else:
            print(f"❌ Archivo no encontrado: {md_file}")
    
    print()
    print("🎉 Proceso completado!")
    print("📁 Archivos generados en el directorio actual")
    
    # Mostrar archivos generados
    import glob
    pdf_files = glob.glob("*.pdf")
    txt_files = glob.glob("*ML_Translate_App.txt")
    
    if pdf_files:
        print("\n📑 PDFs generados:")
        for pdf in pdf_files:
            if os.path.getsize(pdf) > 0:
                print(f"  ✅ {pdf}")
            else:
                print(f"  ⚠️  {pdf} (vacío)")
    
    if txt_files:
        print("\n📄 Archivos de texto generados:")
        for txt in txt_files:
            print(f"  📝 {txt}")

if __name__ == "__main__":
    main()
