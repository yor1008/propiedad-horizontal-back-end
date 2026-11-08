"""Quita el fondo ("sombreado") de las etiquetas de los SVG exportados por Archify.

Archify pinta un rectangulo `c-mask` detras de cada etiqueta de flecha y de cada banda para taparle la linea; esos
fondos a veces cortan la flecha. Se quitan solo los que van inmediatamente antes de un <text>; los `c-mask` de las
barras de activacion no se tocan (van antes de otro <rect>).

Uso:  python docs/diagramas/quitar-fondos-etiquetas.py [archivo.svg ...]   (sin argumentos: todos los de docs/diagramas/svg)
"""
import pathlib
import re
import sys

SVG_DIR = pathlib.Path(__file__).parent / "svg"
FONDO_DE_ETIQUETA = re.compile(r'<rect\b[^>]*class="c-mask"[^>]*/>\s*(?=<text\b)')

archivos = [pathlib.Path(a) for a in sys.argv[1:]] or sorted(SVG_DIR.glob("*.svg"))
for archivo in archivos:
    contenido = archivo.read_text(encoding="utf-8")
    nuevo, quitados = FONDO_DE_ETIQUETA.subn("", contenido)
    archivo.write_text(nuevo, encoding="utf-8")
    print(f"{archivo.name}: {quitados} fondos quitados")
