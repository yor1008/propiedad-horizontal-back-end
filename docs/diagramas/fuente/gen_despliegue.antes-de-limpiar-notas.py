"""Diagrama de despliegue UML de api-propiedad-horizontal: donde corre la aplicacion completa.

Fiel a la infraestructura real del repo:
- Dockerfile: imagen eclipse-temurin:25-jre que ejecuta app.jar (api-propiedad-horizontal-0.0.1-SNAPSHOT.jar);
  Render inyecta PORT (en local 8080).
- .github/workflows/ci.yml: Render despliega solo al detectar el ultimo commit de main.
- Front (repo hermano): proxy Next.js app/api/[...path]/route.ts -> https://propiedad-horizontal-back-end.onrender.com.
- application.properties: PostgreSQL por spring.datasource.* (JDBC), correo por Gmail API (HTTPS, si hay
  GMAIL_REFRESH_TOKEN) o SMTP (MAIL_PORT 587, STARTTLS).
Lo que aun no existe (hosting publico del front, proveedor fijo de la base) va como nota.

Notacion segun OMG UML 2.5.1 (formal/17-12-05):
- 19.4.4: Node = cubo en perspectiva; Device con «device»; ExecutionEnvironment con «executionEnvironment».
  Servicios de terceros como nodos sin palabra clave.
- 19.3.4: Artifact = rectangulo con «artifact»; anidado dentro del nodo donde se despliega (19.4.4), sin «deploy».
- 19.4.4: CommunicationPath = linea de Association (solida, sin flecha), rotulada con protocolo y puerto.
- 9.2.4.1: palabra clave entre comillas angulares, centrada encima del nombre; nombre centrado y en negrita.
- 7.2.4: Comment = rectangulo con la esquina doblada unido por linea punteada al elemento.
- Anexo A: marco con encabezado [<kind>]<name>; para despliegue el kind es "dep".

Uso:
  python docs/diagramas/fuente/gen_despliegue.py
  python docs/diagramas/fuente/secuencia-cu/exportar_drawio_svg.py docs/diagramas/fuente/32-despliegue.drawio.xml docs/diagramas/svg/32-despliegue.svg
"""
import os
import sys
from xml.sax.saxutils import escape

AQUI = os.path.dirname(os.path.abspath(__file__))
SALIDA = sys.argv[1] if len(sys.argv) > 1 else os.path.join(AQUI, "32-despliegue.drawio.xml")
PAGE_W, PAGE_H = 1560, 620

NODO = ("shape=cube;size=12;direction=south;html=1;whiteSpace=wrap;boundedLbl=1;verticalAlign=top;align=center;"
        "spacingTop=4;fontSize=12;strokeColor=#36393d;")
ENTORNO = NODO.replace("size=12", "size=8")
ARTEFACTO = ("rounded=0;html=1;whiteSpace=wrap;fontSize=11;align=center;verticalAlign=middle;"
             "strokeColor=#36393d;fillColor=#ffffff;")
ENLACE = ("edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=none;startArrow=none;strokeColor=#000000;"
          "fontSize=11;labelBackgroundColor=#ffffff;")

celdas = []
nodos = set()   # cajas 3D (direction=south): draw.io gira sus puntos de conexion


def girar(id_, punto):
    """Con direction=south draw.io lleva el punto (x, y) a (1 - y, x); se pasa el inverso para que quede donde se pide."""
    x, y = punto
    return (round(y, 4), round(1 - x, 4)) if id_ in nodos else (x, y)


def caja(id_, x, y, w, h, texto, estilo):
    celdas.append(f'<mxCell id="{id_}" value="{escape(texto, {chr(34): "&quot;"})}" style="{estilo}" vertex="1" '
                  f'parent="1"><mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>')


def nodo(id_, x, y, w, h, palabra, nombre, relleno="#dae8fc", estilo=NODO):
    """Nodo UML: caja 3D con su palabra clave (si tiene) y su nombre."""
    texto = (f"&laquo;{palabra}&raquo;<br>" if palabra else "") + f"<b>{nombre}</b>"
    nodos.add(id_)
    caja(id_, x, y, w, h, texto, estilo + f"fillColor={relleno};")


def artefacto(id_, x, y, w, h, nombre):
    caja(id_, x, y, w, h, f"&laquo;artifact&raquo;<br><b>{nombre}</b>", ARTEFACTO)


def nota(id_, x, y, w, h, texto, anclada_a):
    caja(id_, x, y, w, h, texto, "shape=note;size=14;html=1;whiteSpace=wrap;fontSize=11;align=left;spacingLeft=8;"
         "strokeColor=#36393d;fillColor=#ffffff;")
    celdas.append(f'<mxCell id="a_{id_}" style="endArrow=none;dashed=1;html=1;strokeColor=#36393d;" edge="1" '
                  f'parent="1" source="{id_}" target="{anclada_a}"><mxGeometry relative="1" as="geometry"/></mxCell>')


def enlace(id_, origen, destino, texto, salida, entrada, puntos=()):
    sx, sy = girar(origen, salida)
    ex, ey = girar(destino, entrada)
    pts = "".join(f'<mxPoint x="{x}" y="{y}"/>' for x, y in puntos)
    geo = (f'<mxGeometry relative="1" as="geometry"><Array as="points">{pts}</Array></mxGeometry>' if pts
           else '<mxGeometry relative="1" as="geometry"/>')
    estilo = ENLACE + f"exitX={sx};exitY={sy};exitDx=0;exitDy=0;entryX={ex};entryY={ey};entryDx=0;entryDy=0;"
    celdas.append(f'<mxCell id="{id_}" value="{escape(texto, {chr(34): "&quot;"})}" style="{estilo}" edge="1" '
                  f'parent="1" source="{origen}" target="{destino}">{geo}</mxCell>')


# ---------------------------------------------------------------- marco (Anexo A): encabezado "dep <nombre>"
caja("marco", 20, 20, 1520, 580, "<b>dep</b> api-propiedad-horizontal",
     "shape=umlFrame;whiteSpace=wrap;html=1;width=250;height=30;boundedLbl=1;verticalAlign=middle;align=left;"
     "spacingLeft=8;fontSize=13;fillColor=none;strokeColor=#36393d;")

# ---------------------------------------------------------------- nodos
# Usuario: el navegador solo habla con el front (/api/... en el mismo sitio).
nodo("cli", 40, 130, 220, 200, "device", "Dispositivo del usuario", "#f5f5f5")
nodo("nav", 60, 190, 180, 100, "executionEnvironment", "Navegador web", "#ffffff", ENTORNO)

# Front Next.js: necesita servidor Node (proxy de la API). Hosting publico aun no definido.
nodo("srvfront", 360, 100, 260, 240, "device", "Servidor del front", "#e1d5e7")
nodo("node", 380, 160, 220, 160, "executionEnvironment", "Node.js", "#f8f0fb", ENTORNO)
artefacto("front", 395, 235, 190, 60, "propiedad-horizontal-front-end")
nota("n_front", 360, 370, 260, 50, "Hosting p&uacute;blico pendiente de implementar (hoy <i>pnpm start</i>, :3000)",
     "srvfront")

# Back: Render construye el Dockerfile y ejecuta el contenedor.
nodo("render", 800, 70, 300, 330, "", "Render", "#ffe6cc")
nodo("cont", 820, 130, 260, 220, "executionEnvironment", "Contenedor Docker<br>eclipse-temurin:25-jre",
     "#fff5eb", ENTORNO)
artefacto("jar", 840, 230, 220, 70, "app.jar<br><span style=\"font-weight:normal\">"
          "(api-propiedad-horizontal-0.0.1-SNAPSHOT.jar)</span>")
nota("n_render", 800, 440, 300, 40, "Despliegue autom&aacute;tico al hacer push a <i>main</i>", "render")

# Base de datos: llega por SPRING_DATASOURCE_URL; el proveedor no esta fijado en el repo.
nodo("srvdb", 1240, 80, 280, 150, "device", "Servidor de base de datos", "#d5e8d4")
nodo("pg", 1260, 145, 240, 60, "executionEnvironment", "PostgreSQL", "#ffffff", ENTORNO)
nota("n_db", 1240, 245, 280, 36, "Proveedor seg&uacute;n SPRING_DATASOURCE_URL", "srvdb")

# Correo: uno u otro adaptador de EnviadorCorreo.
nodo("gmail", 1240, 300, 280, 90, "", "Gmail API (Google)", "#f5f5f5")
nodo("smtp", 1240, 440, 280, 90, "", "Servidor SMTP", "#f5f5f5")
nota("n_correo", 1240, 545, 280, 40, "Gmail API si hay GMAIL_REFRESH_TOKEN; si no, SMTP", "smtp")

# ---------------------------------------------------------------- caminos de comunicacion
enlace("c1", "nav", "node", "HTTP :3000<br>/api/...", (1, 0.5), (0, 0.5))
enlace("c2", "node", "cont", "HTTPS :443<br>propiedad-horizontal-back-end<br>.onrender.com", (1, 0.5), (0, 0.5))
enlace("c3", "cont", "pg", "JDBC :5432", (1, 0.2045), (0, 0.5))
enlace("c4", "cont", "gmail", "HTTPS :443", (1, 0.6), (0, 0.5), ((1170, 262), (1170, 345)))
enlace("c5", "cont", "smtp", "SMTP :587<br>STARTTLS", (1, 0.85), (0, 0.5), ((1150, 317), (1150, 485)))

os.makedirs(os.path.dirname(os.path.abspath(SALIDA)), exist_ok=True)
with open(SALIDA, "w", encoding="utf-8") as f:
    f.write('<?xml version="1.0" encoding="UTF-8"?>\n<mxfile host="app.diagrams.net">\n'
            '  <diagram name="Despliegue" id="despliegue">\n'
            f'    <mxGraphModel grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" '
            f'page="1" pageScale="1" pageWidth="{PAGE_W}" pageHeight="{PAGE_H}" background="#ffffff" math="0" '
            'shadow="0">\n      <root>\n        <mxCell id="0"/>\n        <mxCell id="1" parent="0"/>\n'
            + "".join(f"        {c}\n" for c in celdas)
            + "      </root>\n    </mxGraphModel>\n  </diagram>\n</mxfile>\n")
print(SALIDA)
