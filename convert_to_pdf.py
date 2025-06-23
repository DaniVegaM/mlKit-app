#!/usr/bin/env python3
"""
Script para convertir documentos Markdown a PDF
Desarrollado para ML Translate App
Autores: Daniel Vega Miranda & Abraham Reyes Cuevas
"""

import os
import sys
import subprocess

def markdown_to_html(md_file):
    """Convierte Markdown a HTML usando Python"""
    try:
        with open(md_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # HTML básico con estilos
        html_content = f"""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>ML Translate App - Documentation</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            line-height: 1.6;
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
            color: #333;
        }}
        h1 {{
            color: #2c3e50;
            border-bottom: 3px solid #3498db;
            padding-bottom: 10px;
        }}
        h2 {{
            color: #34495e;
            margin-top: 30px;
        }}
        h3 {{
            color: #7f8c8d;
        }}
        code {{
            background-color: #f8f9fa;
            padding: 2px 4px;
            border-radius: 3px;
            font-family: 'Courier New', monospace;
        }}
        pre {{
            background-color: #f8f9fa;
            padding: 15px;
            border-radius: 5px;
            overflow-x: auto;
            border-left: 4px solid #3498db;
        }}
        blockquote {{
            border-left: 4px solid #3498db;
            margin: 0;
            padding-left: 20px;
            color: #7f8c8d;
        }}
        .emoji {{
            font-size: 1.2em;
        }}
        table {{
            border-collapse: collapse;
            width: 100%;
            margin: 20px 0;
        }}
        th, td {{
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }}
        th {{
            background-color: #f8f9fa;
        }}
    </style>
</head>
<body>
"""
        
        # Conversión básica de Markdown a HTML
        lines = content.split('\n')
        in_code_block = False
        
        for line in lines:
            line = line.rstrip()
            
            if line.startswith('```'):
                if in_code_block:
                    html_content += "</pre>\n"
                    in_code_block = False
                else:
                    html_content += "<pre><code>\n"
                    in_code_block = True
                continue
            
            if in_code_block:
                html_content += line + "\n"
                continue
            
            if line.startswith('# '):
                html_content += f"<h1>{line[2:]}</h1>\n"
            elif line.startswith('## '):
                html_content += f"<h2>{line[3:]}</h2>\n"
            elif line.startswith('### '):
                html_content += f"<h3>{line[4:]}</h3>\n"
            elif line.startswith('#### '):
                html_content += f"<h4>{line[5:]}</h4>\n"
            elif line.startswith('- '):
                html_content += f"<li>{line[2:]}</li>\n"
            elif line.startswith('**') and line.endswith('**'):
                html_content += f"<p><strong>{line[2:-2]}</strong></p>\n"
            elif line.startswith('*') and line.endswith('*'):
                html_content += f"<p><em>{line[1:-1]}</em></p>\n"
            elif line.strip() == '---':
                html_content += "<hr>\n"
            elif line.strip() == '':
                html_content += "<br>\n"
            else:
                html_content += f"<p>{line}</p>\n"
        
        html_content += """
</body>
</html>
"""
        
        html_file = md_file.replace('.md', '.html')
        with open(html_file, 'w', encoding='utf-8') as f:
            f.write(html_content)
        
        return html_file
    
    except Exception as e:
        print(f"Error converting {md_file} to HTML: {e}")
        return None

def html_to_pdf_safari(html_file):
    """Convierte HTML a PDF usando Safari/WebKit en macOS"""
    try:
        pdf_file = html_file.replace('.html', '.pdf')
        
        # Usar Safari para generar PDF
        applescript = f'''
tell application "Safari"
    activate
    open (POSIX file "{os.path.abspath(html_file)}")
    delay 3
    tell application "System Events"
        keystroke "p" using command down
        delay 2
        keystroke return
        delay 1
        keystroke "s" using command down
        delay 1
        keystroke "{pdf_file}"
        delay 1
        keystroke return
    end tell
    delay 3
    close front window
end tell
'''
        
        # Intentar método alternativo usando wkhtmltopdf si está disponible
        result = subprocess.run(['which', 'wkhtmltopdf'], capture_output=True)
        if result.returncode == 0:
            subprocess.run(['wkhtmltopdf', html_file, pdf_file])
            return pdf_file
        
        # Usar textutil como fallback para macOS
        subprocess.run(['textutil', '-convert', 'pdf', html_file, '-output', pdf_file])
        return pdf_file
        
    except Exception as e:
        print(f"Error converting {html_file} to PDF: {e}")
        return None

def main():
    files_to_convert = [
        'Manual_de_Usuario_ML_Translate_App.md',
        'Documentacion_Tecnica_ML_Translate_App.md'
    ]
    
    for md_file in files_to_convert:
        if os.path.exists(md_file):
            print(f"Convirtiendo {md_file}...")
            html_file = markdown_to_html(md_file)
            if html_file:
                pdf_file = html_to_pdf_safari(html_file)
                if pdf_file:
                    print(f"✅ PDF generado: {pdf_file}")
                else:
                    print(f"❌ Error generando PDF para {md_file}")
            else:
                print(f"❌ Error convirtiendo {md_file} a HTML")
        else:
            print(f"❌ Archivo no encontrado: {md_file}")

if __name__ == "__main__":
    main()
