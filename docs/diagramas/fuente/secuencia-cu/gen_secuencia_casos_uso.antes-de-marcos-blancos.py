#!/usr/bin/env python3
"""
Genera un diagrama de secuencia por caso de uso (CU-001 ... CU-031):
  - docs/diagramas/fuente/secuencia-cu/secuencia-casos-de-uso.xml  (draw.io, una pagina por caso de uso; es la fuente)
  - docs/diagramas/Secuencia/<cu-nnn-nombre>.svg                   (un SVG por pagina, con el visor oficial de draw.io)

Reglas (skill diagramas-uml, seccion 3):
  - Participantes = actor del diagrama de casos de uso + clases del diagrama de clases
    (docs/diagramas/Clases/DIAGRAMA DE CLASE.mejorado.xml). Mensajes = SUS operaciones, con el nombre y los
    parametros del diagrama de clases. Cada flecha es una llamada que el codigo hace de verdad; lo que orquesta la
    aplicacion (servicios, repositorios, controladores) no aparece: sale del actor.
  - Notacion OMG UML 2.5.1 (formal/17-12-05):
      17.2.4.1  la interaccion va en un marco con el pentagono "sd <nombre>" (tabla 17.1, "Frame");
      17.3.4.1  linea de vida = cabeza + linea punteada; el actor usa su figura;
      17.2.4.4  barras de ejecucion delgadas y blancas;
      17.4.4.1  llamada sincrona = punta llena; retorno = punteada con etiqueta "operacion(): valor"
                (o sin etiqueta si la operacion no devuelve nada); creacion = punteada con punta abierta
                hacia la cabeza del objeto creado;
      17.6.4    alt/opt/loop con guardas [..] y la ultima rama [else]; operandos separados por linea punteada;
      17.7.4    "ref" (InteractionUse) para los casos de uso que extienden a otro («extend»).

Uso:  python docs/diagramas/fuente/secuencia-cu/gen_secuencia_casos_uso.py                  (solo los SVG)
      python docs/diagramas/fuente/secuencia-cu/gen_secuencia_casos_uso.py --regenerar-xml  (tambien el XML; descarta
                                                                                          los retoques hechos a mano)
"""
import html
import os
import re
import sys
import unicodedata

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from conectar_secuencia import conectar_xml  # noqa: E402

AQUI = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(AQUI, "secuencia-casos-de-uso.xml")

ESPACIO = 270          # distancia entre lineas de vida
ANCHO_CAJA = 150       # ancho minimo de la cabeza de cada linea de vida
CABEZA = 40            # alto de la cabeza
TOP = 60               # y de las cabezas
Y0 = 170               # y de la primera interaccion
CHAR = 6.4             # ancho aproximado de un caracter (px) a 11 px

CABEZA_RELLENO = "#d5e8d4"   # verde: paleta propia de este proyecto (otros proyectos usan azul/naranja)
CABEZA_BORDE = "#82b366"
ACTOR_RELLENO = "#fff2cc"
ACTOR_BORDE = "#d6b656"
BORDE = "#36393d"


def esc(s):
    """Escapa una cadena para ponerla en un atributo XML."""
    return html.escape(s, quote=True)


def etiqueta_html(lineas):
    """Texto de una etiqueta html=1 de draw.io; se escapa PRIMERO como HTML (si no, 'List<X>' pierde '<X>')."""
    if isinstance(lineas, str):
        lineas = [lineas]
    return "<br>".join(html.escape(linea, quote=False) for linea in lineas)


def envolver(texto, ancho_px):
    """Parte el texto en lineas cortando en ', ' o espacios."""
    maxc = max(12, int(ancho_px / CHAR))
    if len(texto) <= maxc:
        return [texto]
    palabras = texto.replace(", ", ",\u0001").split("\u0001")
    lineas, actual = [], ""
    for p in palabras:
        if actual and len(actual) + len(p) + 1 > maxc:
            lineas.append(actual.rstrip())
            actual = p
        else:
            actual = (actual + " " + p) if actual else p
    if actual:
        lineas.append(actual.strip())
    return lineas


def nombre_operacion(op):
    return op.split("(")[0].strip()


class Diagrama:
    def __init__(self, nombre, participantes):
        """participantes: lista de (id, tipo, etiqueta); tipo = 'actor' | 'obj'."""
        self.nombre = nombre
        self.parts = {}
        self.orden = []
        for i, (pid, tipo, etiqueta) in enumerate(participantes):
            cx = 110 + i * ESPACIO
            ancho = max(ANCHO_CAJA, int(len(etiqueta) * 7.6) + 24)
            self.parts[pid] = dict(tipo=tipo, etiqueta=etiqueta, cx=cx, top=TOP, ancho=ancho)
            self.orden.append(pid)
        self.y = Y0
        self.celdas_frames, self.celdas_act, self.celdas_msg, self.celdas_txt = [], [], [], []
        self.pila_frames = []
        self.activaciones = {pid: [] for pid in self.orden}
        self.ops = {pid: [] for pid in self.orden}
        self._n = 0
        self.max_x = 110 + (len(participantes) - 1) * ESPACIO + 120

    # ------------------------------------------------------------ utilidades
    def _id(self, pref):
        self._n += 1
        return f"{pref}{self._n}"

    def cx(self, pid):
        return self.parts[pid]["cx"]

    def _tocar(self, x1, x2):
        self.max_x = max(self.max_x, x2)
        if self.pila_frames:
            f = self.pila_frames[-1]
            f["min"] = min(f["min"], x1)
            f["max"] = max(f["max"], x2)

    # ------------------------------------------------------------ mensajes
    def _mensaje(self, a, b, etiqueta, tipo):
        """tipo: 'llamada' | 'retorno' | 'creacion'."""
        xa, xb = self.cx(a), self.cx(b)
        if tipo == "creacion":       # la punta llega al borde de la cabeza del objeto creado
            mitad = self.parts[b]["ancho"] / 2
            xb = xb - mitad if xb > xa else xb + mitad
        ancho = abs(xb - xa) - 30
        lineas = envolver(etiqueta, ancho) if etiqueta else [""]
        h = 15 * len(lineas)
        y = self.y + h + 8
        da = 0 if self.parts[a]["tipo"] == "actor" else 5
        db = 0 if (self.parts[b]["tipo"] == "actor" or tipo == "creacion") else 5
        if xb > xa:
            sx, tx = xa + da, xb - db
        else:
            sx, tx = xa - da, xb + db
        if tipo == "llamada":
            punta = "endArrow=block;endFill=1;endSize=8;"
        else:                         # retorno y creacion: punteada con punta abierta
            punta = "dashed=1;endArrow=open;endFill=0;endSize=8;"
        estilo = "html=1;verticalAlign=bottom;rounded=0;fontSize=11;strokeColor=#000000;" + punta
        self.celdas_msg.append(
            f'<mxCell id="{self._id("m")}" value="{esc(etiqueta_html(lineas))}" style="{estilo}" '
            f'edge="1" parent="1"><mxGeometry relative="1" as="geometry">'
            f'<mxPoint x="{sx}" y="{y}" as="sourcePoint"/><mxPoint x="{tx}" y="{y}" as="targetPoint"/>'
            f'</mxGeometry></mxCell>')
        self._tocar(min(sx, tx) - 25, max(sx, tx) + 25)
        self.y = y + 14
        return y

    def llamar(self, a, b, op):
        """Llamada sincrona a la operacion op; deja abierta la ejecucion de b hasta su retorno."""
        y = self._mensaje(a, b, op, "llamada")
        self.activaciones[b].append(y)
        self.ops[b].append(op)

    def retornar(self, b, a, valor=None, cerrar=True):
        """Retorno de b hacia a. Etiqueta '<operacion>(): <valor>' (17.4.4.1) o vacia si no devuelve nada.
        cerrar=False en todas las ramas de un alt menos la ultima: la ejecucion sigue abierta hasta el ultimo retorno."""
        op = (self.ops[b].pop() if cerrar else self.ops[b][-1]) if self.ops[b] else ""
        etiqueta = f"{nombre_operacion(op)}(): {valor}" if valor is not None else ""
        y = self._mensaje(b, a, etiqueta, "retorno")
        if cerrar and self.activaciones[b]:
            y0 = self.activaciones[b].pop()
            self._activacion(b, y0 - 6, y + 6)

    def llamar_y_retornar(self, a, b, op, valor=None):
        self.llamar(a, b, op)
        self.retornar(b, a, valor)

    def crear(self, a, b):
        """Mensaje de creacion: la linea de vida de b empieza en este punto (su cabeza baja hasta la flecha)."""
        y = self._mensaje(a, b, "«create»", "creacion")
        self.parts[b]["top"] = y - CABEZA / 2
        cx, mitad = self.cx(b), self.parts[b]["ancho"] / 2
        self._tocar(cx - mitad, cx + mitad)
        self.y = y + CABEZA / 2 + 12

    def propio(self, a, op):
        """Mensaje de un objeto a si mismo (lazo a la derecha con su ejecucion anidada)."""
        cx = self.cx(a)
        ancho_txt = ESPACIO - 85
        lineas = envolver(op, ancho_txt)
        alto = max(24, 15 * len(lineas) + 4)
        y = self.y + 12
        x1 = cx + 5
        self.celdas_msg.append(
            f'<mxCell id="{self._id("s")}" value="" style="html=1;rounded=0;endArrow=block;endFill=1;'
            f'endSize=8;strokeColor=#000000;" edge="1" parent="1"><mxGeometry relative="1" as="geometry">'
            f'<mxPoint x="{x1}" y="{y}" as="sourcePoint"/><mxPoint x="{x1 + 5}" y="{y + alto}" as="targetPoint"/>'
            f'<Array as="points"><mxPoint x="{x1 + 38}" y="{y}"/><mxPoint x="{x1 + 38}" y="{y + alto}"/>'
            f'</Array></mxGeometry></mxCell>')
        self._texto(x1 + 44, y - 2, ancho_txt, alto + 4, etiqueta_html(lineas), "left")
        self._activacion(a, y + alto - 4, y + alto + 14, desplazo=5)
        self._tocar(cx - 25, x1 + 44 + min(ancho_txt, int(max(len(l) for l in lineas) * CHAR)) + 15)
        self.y = y + alto + 22

    def _activacion(self, pid, y1, y2, desplazo=0):
        x = self.cx(pid) - 5 + desplazo
        self.celdas_act.append(
            f'<mxCell id="{self._id("a")}" value="" style="html=1;points=[];perimeter=orthogonalPerimeter;'
            f'fillColor=#ffffff;strokeColor=#000000;" vertex="1" parent="1">'
            f'<mxGeometry x="{x}" y="{y1}" width="10" height="{max(14, y2 - y1)}" as="geometry"/></mxCell>')

    def _texto(self, x, y, w, h, valor, align="left", estilo_extra=""):
        self.celdas_txt.append(
            f'<mxCell id="{self._id("t")}" value="{esc(valor)}" style="text;html=1;align={align};'
            f'verticalAlign=top;whiteSpace=wrap;overflow=hidden;fontSize=11;{estilo_extra}" '
            f'vertex="1" parent="1"><mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>')

    # ------------------------------------------------------------ fragmentos
    def abrir(self, tipo, condicion):
        self.y += 14
        self.pila_frames.append(dict(tipo=tipo, top=self.y, min=10 ** 6, max=0, ramas=[], cond=condicion))
        self.y += 34

    def sino(self, condicion="else"):
        f = self.pila_frames[-1]
        self.y += 6
        f["ramas"].append((self.y, condicion))
        self.y += 34

    def cerrar(self):
        f = self.pila_frames.pop()
        self.y += 8
        x1, x2 = f["min"] - 30, f["max"] + 15
        if x1 > x2:
            x1, x2 = 10, 200
        x1 = max(20, x1)
        top, bottom = f["top"], self.y
        ancho = x2 - x1
        self.celdas_frames.append(
            f'<mxCell id="{self._id("f")}" value="{esc(etiqueta_html(f["tipo"]))}" style="shape=umlFrame;'
            f'whiteSpace=wrap;html=1;pointerEvents=0;fillColor=none;strokeColor=#36393d;fontStyle=1;fontSize=11;'
            f'width=55;height=22;" vertex="1" parent="1"><mxGeometry x="{x1}" y="{top}" width="{ancho}" '
            f'height="{bottom - top}" as="geometry"/></mxCell>')
        self._guarda(x1 + 62, top + 3, max(120, ancho - 70), f["cond"])
        for ry, cond in f["ramas"]:
            self.celdas_frames.append(
                f'<mxCell id="{self._id("d")}" value="" style="endArrow=none;dashed=1;html=1;strokeColor=#36393d;" '
                f'edge="1" parent="1"><mxGeometry relative="1" as="geometry">'
                f'<mxPoint x="{x1}" y="{ry}" as="sourcePoint"/><mxPoint x="{x2}" y="{ry}" as="targetPoint"/>'
                f'</mxGeometry></mxCell>')
            self._guarda(x1 + 8, ry + 3, max(120, ancho - 20), cond)
        self.y += 14
        if self.pila_frames:
            p = self.pila_frames[-1]
            p["min"] = min(p["min"], x1)
            p["max"] = max(p["max"], x2)
        self.max_x = max(self.max_x, x2 + 20)

    def _guarda(self, x, y, w, cond):
        self.celdas_txt.append(
            f'<mxCell id="{self._id("g")}" value="{esc(etiqueta_html("[" + cond + "]"))}" style="text;html=1;'
            f'align=left;verticalAlign=top;whiteSpace=wrap;fontSize=11;fontStyle=2;" vertex="1" parent="1">'
            f'<mxGeometry x="{x}" y="{y}" width="{w}" height="22" as="geometry"/></mxCell>')

    def ref(self, interaccion, desde, hasta):
        """InteractionUse (17.7.4): marco 'ref' con el nombre de la interaccion referida, sobre las lineas de vida."""
        x1 = min(self.cx(desde), self.cx(hasta)) - 70
        x2 = max(self.cx(desde), self.cx(hasta)) + 70
        top = self.y + 14
        alto = 54
        self.celdas_frames.append(
            f'<mxCell id="{self._id("f")}" value="ref" style="shape=umlFrame;whiteSpace=wrap;html=1;'
            f'pointerEvents=0;fillColor=none;strokeColor=#36393d;fontStyle=1;fontSize=11;width=40;height=20;" '
            f'vertex="1" parent="1"><mxGeometry x="{x1}" y="{top}" width="{x2 - x1}" height="{alto}" '
            f'as="geometry"/></mxCell>')
        self.celdas_txt.append(
            f'<mxCell id="{self._id("t")}" value="{esc(etiqueta_html(interaccion))}" style="text;html=1;'
            f'align=center;verticalAlign=middle;whiteSpace=wrap;fontSize=11;" vertex="1" parent="1">'
            f'<mxGeometry x="{x1 + 44}" y="{top + 18}" width="{x2 - x1 - 52}" height="28" as="geometry"/></mxCell>')
        self._tocar(x1, x2)
        self.y = top + alto + 10

    # ------------------------------------------------------------ salida XML
    def xml(self):
        texto, avisos, _ = conectar_xml(self._xml_crudo())
        for a in avisos:
            print("AVISO", a)
        return "  " + texto.strip() + "\n"

    def _xml_crudo(self):
        fin = self.y + 30
        celdas = ['<mxCell id="0"/>', '<mxCell id="1" parent="0"/>']
        for pid in self.orden:
            p = self.parts[pid]
            cx = p["cx"]
            if p["tipo"] == "actor":
                celdas.append(
                    f'<mxCell id="L-{pid}" value="{esc(etiqueta_html(p["etiqueta"]))}" style="shape=umlActor;'
                    f'verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;fontStyle=1;'
                    f'fontSize=11;whiteSpace=wrap;fillColor={ACTOR_RELLENO};strokeColor={ACTOR_BORDE};" vertex="1" parent="1">'
                    f'<mxGeometry x="{cx - 14}" y="{TOP}" width="28" height="56" as="geometry"/></mxCell>')
                celdas.append(
                    f'<mxCell id="V-{pid}" value="" style="endArrow=none;dashed=1;html=1;strokeColor=#666666;" '
                    f'edge="1" parent="1"><mxGeometry relative="1" as="geometry">'
                    f'<mxPoint x="{cx}" y="{Y0 - 14}" as="sourcePoint"/><mxPoint x="{cx}" y="{fin}" as="targetPoint"/>'
                    f'</mxGeometry></mxCell>')
            else:
                top, ancho = p["top"], p["ancho"]
                celdas.append(
                    f'<mxCell id="L-{pid}" value="{esc(etiqueta_html(p["etiqueta"]))}" style="shape=umlLifeline;'
                    f'perimeter=lifelinePerimeter;whiteSpace=wrap;html=1;container=0;dropTarget=0;size={CABEZA};'
                    f'collapsible=0;recursiveResize=0;outlineConnect=0;portConstraint=eastwest;'
                    f'fillColor={CABEZA_RELLENO};strokeColor={CABEZA_BORDE};fontStyle=1;fontSize=12;" vertex="1" parent="1">'
                    f'<mxGeometry x="{cx - ancho / 2:g}" y="{top:g}" width="{ancho}" '
                    f'height="{fin - top:g}" as="geometry"/></mxCell>')
        # marco de la interaccion: pentagono "sd <nombre>" (17.2.4.1)
        sd = f"sd {self.nombre}"
        ancho_sd = int(len(sd) * 7.4) + 24
        ancho = self.max_x + 10
        celdas.append(
            f'<mxCell id="sd" value="{esc(etiqueta_html(sd))}" style="shape=umlFrame;whiteSpace=wrap;html=1;'
            f'pointerEvents=0;fillColor=none;strokeColor=#000000;fontStyle=1;fontSize=12;align=left;spacingLeft=6;'
            f'width={ancho_sd};height=26;" vertex="1" parent="1">'
            f'<mxGeometry x="10" y="10" width="{ancho}" height="{fin + 10}" as="geometry"/></mxCell>')
        celdas += self.celdas_act + self.celdas_msg + self.celdas_txt + self.celdas_frames
        cuerpo = "\n        ".join(celdas)
        return (f'  <diagram name="{esc(self.nombre)}" id="{esc(nombre_archivo(self.nombre))}">\n'
                f'    <mxGraphModel dx="1400" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" '
                f'arrows="1" fold="1" page="1" pageScale="1" pageWidth="{ancho + 40}" pageHeight="{fin + 60}" '
                f'math="0" shadow="0">\n      <root>\n        {cuerpo}\n      </root>\n    </mxGraphModel>\n'
                f'  </diagram>\n')


def nombre_archivo(nombre):
    base = unicodedata.normalize("NFD", nombre.lower())
    base = "".join(ch for ch in base if unicodedata.category(ch) != "Mn")
    return re.sub(r"[^a-z0-9]+", "-", base).strip("-")


# =========================================================================================================
#  PARTICIPANTES Y AYUDAS
# =========================================================================================================
def actor(nombre):
    return ("act", "actor", nombre)


def o(pid, clase, rol=""):
    return (pid, "obj", f"{rol}:{clase}")


diagramas = []


def nuevo(nombre, participantes):
    d = Diagrama(nombre, participantes)
    diagramas.append(d)
    return d


def transicionar(d, ent, est="est", tra="tra"):
    """Lo que hace GestorEstados.transicionar: el estado actual valida el destino y la entidad cambia de estado."""
    d.llamar("act", est, "puedeCambiarA(destino)")
    d.abrir("loop", "por cada transición del estado actual")
    d.llamar_y_retornar(est, tra, "esPermitida(origen, destino)", "permitida")
    d.cerrar()
    d.retornar(est, "act", "true")
    d.llamar_y_retornar("act", ent, "cambiarEstado(nuevo)")


def notificar(d, cfg="cfg"):
    """Notificador: la configuracion del evento genera las notificaciones de los destinatarios."""
    d.llamar_y_retornar("act", cfg, "generar(destinatarios, mensaje)", "notificaciones")


def recalcular_ocupacion(d, per="per", inm="inm"):
    """OcupacionInmuebles.recalcular: ocupado si alguna persona del inmueble con vinculo vigente es residente."""
    d.abrir("loop", "por cada persona del inmueble")
    d.llamar_y_retornar("act", per, "vinculoEstaVigente()", "vigente")
    d.cerrar()
    d.llamar_y_retornar("act", inm, "marcarOcupado(ocupado)")


def propietario_vigente(d, inm="inm", per="per"):
    """Inmueble.obtenerPropietarioVigente: primera persona que es propietaria (con vinculo vigente) del inmueble."""
    d.llamar("act", inm, "obtenerPropietarioVigente()")
    d.abrir("loop", "por cada persona del inmueble, hasta el primer propietario")
    d.llamar(inm, per, "esPropietarioDe(otro)")
    d.propio(per, "vinculoEstaVigente()")
    d.retornar(per, inm, "esPropietario")
    d.cerrar()
    d.retornar(inm, "act", "propietario")


# Regla: cada flecha es una llamada que el codigo hace de verdad (src/main/java/.../domain/model y los servicios que
# las orquestan), con el nombre y los parametros del diagrama de clases. Lo que orquesta la aplicacion sale del actor.

# ================================================================ Modulo transversal (CU-001 ... CU-007)
# Iniciar sesion, cambiar y restablecer la contrasena los orquesta la aplicacion (BCrypt y JWT); la entidad solo
# registra el acceso o guarda el nuevo hash. Cerrar sesion es descartar el token en el cliente (sin llamada).
d = nuevo("CU-001 Gestionar sesión", [actor("Usuario registrado"), o("cta", "CuentaUsuario")])
d.abrir("opt", "cuenta activa y contraseña correcta")
d.llamar_y_retornar("act", "cta", "registrarAcceso(momento)")
d.cerrar()

d = nuevo("CU-002 Gestionar contraseña", [actor("Usuario registrado"), o("cta", "CuentaUsuario")])
d.abrir("alt", "cambiar la contraseña con la sesión iniciada y la contraseña actual correcta")
d.llamar_y_retornar("act", "cta", "actualizarHashContrasena(nuevoHash)")
d.sino("olvidó la contraseña: token válido, vigente y sin usar")
d.llamar_y_retornar("act", "cta", "actualizarHashContrasena(nuevoHash)")
d.cerrar()

d = nuevo("CU-003 Gestionar datos propios", [actor("Persona vinculada"), o("per", "Persona")])
d.llamar("act", "per", "actualizarDatos(nombres, apellidos, telefono, correo)")
d.propio("per", "actualizarContacto(telefono, correo)")
d.retornar("per", "act")

d = nuevo("CU-004 Consultar notificaciones", [actor("Persona vinculada"), o("not", "Notificacion")])
d.abrir("loop", "por cada notificación propia que se marca como leída")
d.llamar_y_retornar("act", "not", "marcarLeida()")
d.cerrar()

d = nuevo("CU-005 Gestionar cuentas de usuario", [actor("Administrador"), o("cta", "CuentaUsuario")])
d.abrir("alt", "rol ADMINISTRADOR o VIGILANTE")
d.llamar_y_retornar("act", "cta", "crearCuentaPersonal(correo, contrasena, rol)", "cuenta")
d.sino("rol PERSONA_VINCULADA")
d.llamar("act", "cta", "crearCuentaPersonaVinculada(correo, contrasena, persona)")
d.retornar("cta", "act", "cuenta")
d.cerrar()
d.abrir("alt", "desactivar la cuenta")
d.llamar_y_retornar("act", "cta", "desactivar()")
d.sino("reactivar la cuenta")
d.llamar_y_retornar("act", "cta", "reactivar()")
d.cerrar()

d = nuevo("CU-006 Administrar catálogos", [actor("Administrador"), o("val", "ValorCatalogo")])
d.llamar_y_retornar("act", "val", "listarActivos(tipo)", "valores")
d.abrir("alt", "crear un valor")
d.llamar_y_retornar("act", "val", "registrar(tipo, nombre)")
d.sino("renombrar un valor")
d.llamar_y_retornar("act", "val", "modificar(nombre)")
d.sino("desactivar un valor")
d.llamar_y_retornar("act", "val", "desactivar()")
d.sino("reactivar un valor")
d.llamar_y_retornar("act", "val", "reactivar()")
d.cerrar()

d = nuevo("CU-007 Configurar parámetros y notificaciones",
          [actor("Administrador"), o("gen", "ConfiguracionGeneral"), o("cfg", "ConfiguracionNotificacion")])
d.abrir("opt", "todavía no existe la fila de parámetros")
d.llamar_y_retornar("act", "gen", "porDefecto()", "configuracion")
d.cerrar()
d.llamar_y_retornar("act", "gen", "actualizar(maxVehiculos, plazoPqrs)")
d.abrir("loop", "por cada evento que se configura")
d.llamar_y_retornar("act", "cfg", "configurar(activa, enviarCorreo)")
d.cerrar()

# ================================================================ Modulo administracion (CU-008 ... CU-014)
d = nuevo("CU-008 Gestionar personas",
          [actor("Administrador"), o("ver", "VerificacionCorreo"), o("per", "Persona"), o("inm", "Inmueble")])
d.abrir("opt", "la persona tiene correo")
d.abrir("alt", "no hay un código pendiente para ese correo")
d.crear("act", "ver")
d.sino()
d.llamar_y_retornar("act", "ver", "renovar(hashCodigo, expiraEn)")
d.cerrar()
d.llamar_y_retornar("act", "ver", "estaVencido(ahora)", "false")
d.abrir("opt", "el código no coincide")
d.llamar_y_retornar("act", "ver", "registrarIntentoFallido()")
d.cerrar()
d.cerrar()
d.llamar_y_retornar("act", "per",
                    "registrar(tipoDoc, numeroDoc, nombres, apellidos, telefono, correo, autorizaDatos)")
d.abrir("alt", "editar la persona")
d.llamar("act", "per", "actualizarDatos(nombres, apellidos, telefono, correo)")
d.propio("per", "actualizarContacto(telefono, correo)")
d.retornar("per", "act")
d.sino("desactivar la persona")
d.llamar_y_retornar("act", "per", "desactivar(motivo)")
d.abrir("opt", "la persona tiene inmueble")
recalcular_ocupacion(d)
d.cerrar()
d.sino("reactivar la persona")
d.llamar_y_retornar("act", "per", "reactivar()")
d.abrir("opt", "la persona tiene inmueble")
recalcular_ocupacion(d)
d.cerrar()
d.cerrar()

d = nuevo("CU-009 Gestionar inmuebles", [actor("Administrador"), o("inm", "Inmueble"), o("per", "Persona")])
d.llamar_y_retornar("act", "inm", "registrar(torre, numero, piso, tipo)")
d.abrir("loop", "por cada inmueble del listado")
propietario_vigente(d)
d.cerrar()
d.abrir("alt", "modificar el inmueble")
d.llamar_y_retornar("act", "inm", "modificarDatos(torreBloque, numero, piso, tipo)")
d.sino("desactivar el inmueble")
d.abrir("loop", "por cada persona del inmueble")
d.llamar_y_retornar("act", "per", "vinculoEstaVigente()", "false")
d.cerrar()
d.llamar_y_retornar("act", "inm", "desactivar(motivo)")
d.sino("reactivar el inmueble")
d.llamar_y_retornar("act", "inm", "reactivar()")
d.cerrar()
propietario_vigente(d)

d = nuevo("CU-010 Gestionar vinculaciones",
          [actor("Administrador"), o("tip", "TipoPersona"), o("per", "Persona"), o("inm", "Inmueble")])
d.abrir("alt", "vincular una persona a un inmueble")
d.llamar_y_retornar("act", "tip", "listarActivos()", "tipos")
d.llamar_y_retornar("act", "per", "vinculoEstaVigente()", "false")
d.llamar_y_retornar("act", "per", "vincularInmueble(inmueble, tipo, fechaInicio)")
d.sino("finalizar el vínculo")
d.llamar_y_retornar("act", "per", "vinculoEstaVigente()", "true")
d.llamar_y_retornar("act", "per", "finalizarVinculo(fechaFin, motivo)")
d.cerrar()
recalcular_ocupacion(d)

d = nuevo("CU-011 Consultar inmuebles propios",
          [actor("Propietario"), o("per", "Persona"), o("inm", "Inmueble")])
d.llamar_y_retornar("act", "per", "vinculoEstaVigente()", "vigente")
d.abrir("opt", "vínculo vigente y el tipo de persona es propietario")
d.llamar_y_retornar("act", "inm", "obtenerParqueaderos()", "parqueaderos")
d.cerrar()

d = nuevo("CU-012 Gestionar vehículos",
          [actor("Administrador"), o("veh", "Vehiculo"), o("per", "Persona")])
d.abrir("alt", "registrar un vehículo")
d.llamar_y_retornar("act", "veh", "buscarPorPlaca(placa)", "vacio")
d.llamar_y_retornar("act", "per", "vinculoEstaVigente()", "true")
d.llamar_y_retornar("act", "veh", "registrar(placa, tipo, marca, color)")
d.sino("modificar un vehículo")
d.llamar_y_retornar("act", "veh", "modificarDatos(tipo, marca, color)")
d.sino("desactivar un vehículo")
d.llamar_y_retornar("act", "veh", "desactivar(motivo)")
d.cerrar()

d = nuevo("CU-013 Consultar vehículo por placa", [actor("Vigilante"), o("veh", "Vehiculo")])
d.llamar_y_retornar("act", "veh", "buscarPorPlaca(placa)", "vehiculo")

d = nuevo("CU-014 Gestionar parqueaderos",
          [actor("Administrador"), o("par", "Inmueble", "parqueadero"), o("uni", "Inmueble", "unidad")])
d.abrir("alt", "crear un parqueadero")
d.llamar_y_retornar("act", "par", "registrar(torre, numero, piso, tipo)")
d.sino("asignar el parqueadero a una unidad")
d.llamar_y_retornar("act", "uni", "asignarParqueadero(parqueadero)")
d.sino("liberar el parqueadero")
d.llamar_y_retornar("act", "uni", "retirarParqueadero(parqueadero)")
d.sino("desactivar el parqueadero")
d.llamar_y_retornar("act", "par", "desactivar(motivo)")
d.cerrar()

# ================================================================ Modulo seguridad (CU-015 ... CU-021)
d = nuevo("CU-015 Gestionar pre-registros de visitantes",
          [actor("Residente"), o("per", "Persona"), o("vis", "Visitante"), o("pre", "PreRegistro")])
d.abrir("alt", "crear un pre-registro")
d.llamar_y_retornar("act", "per", "vinculoEstaVigente()", "true")
d.llamar_y_retornar("act", "vis", "buscarPorDocumento(documento)", "visitante")
d.abrir("opt", "el visitante no existe")
d.llamar_y_retornar("act", "vis", "registrar(nombre, documento)")
d.cerrar()
d.crear("act", "pre")
d.sino("cancelar un pre-registro")
d.llamar_y_retornar("act", "pre", "cancelar()")
d.cerrar()

d = nuevo("CU-016 Autorizar ingreso de visitante",
          [actor("Vigilante"), o("vis", "Visitante"), o("pre", "PreRegistro"), o("per", "Persona", "residente"),
           o("reg", "RegistroVisita"), o("cfg", "ConfiguracionNotificacion")])
d.llamar_y_retornar("act", "vis", "buscarPorDocumento(documento)", "visitante")
d.abrir("loop", "por cada pre-registro del visitante")
d.llamar_y_retornar("act", "pre", "estaVigente(fecha)", "vigente")
d.cerrar()
d.abrir("alt", "tiene pre-registro vigente")
d.llamar_y_retornar("act", "pre", "estaVigente(fecha)", "true")
d.llamar_y_retornar("act", "per", "vinculoEstaVigente()", "true")
d.crear("act", "reg")
d.llamar_y_retornar("act", "reg", "autorizarIngreso()")
d.abrir("opt", "pre-registro de FECHA_ESPECIFICA")
d.llamar_y_retornar("act", "pre", "marcarUsado()")
d.cerrar()
notificar(d)
d.sino()
d.ref("CU-017 Autorizar ingreso sin pre-registro", "act", "cfg")
d.cerrar()
d.abrir("opt", "el visitante necesita parqueadero")
d.ref("CU-018 Asignar parqueadero de visitante", "act", "reg")
d.cerrar()

d = nuevo("CU-017 Autorizar ingreso sin pre-registro",
          [actor("Vigilante"), o("per", "Persona", "residente"), o("vis", "Visitante"), o("reg", "RegistroVisita"),
           o("cfg", "ConfiguracionNotificacion")])
d.llamar("act", "per", "esResidenteDe(otro)")
d.propio("per", "vinculoEstaVigente()")
d.retornar("per", "act", "true")
d.llamar_y_retornar("act", "vis", "buscarPorDocumento(documento)", "visitante")
d.abrir("opt", "el visitante no existe")
d.llamar_y_retornar("act", "vis", "registrar(nombre, documento)")
d.cerrar()
d.crear("act", "reg")
d.llamar_y_retornar("act", "reg", "registrarContactoResidente(residente, respuesta)")
d.abrir("alt", "respuesta = APRUEBA")
d.llamar_y_retornar("act", "reg", "autorizarIngreso()")
notificar(d)
d.sino("respuesta = NO_APRUEBA o NO_CONTESTA")
d.llamar_y_retornar("act", "reg", "negarIngreso(motivo)")
d.cerrar()

d = nuevo("CU-018 Asignar parqueadero de visitante", [actor("Vigilante"), o("reg", "RegistroVisita")])
d.llamar_y_retornar("act", "reg", "estaDentro()", "dentro")
d.abrir("opt", "el visitante está dentro y el parqueadero es de visitantes y está libre")
d.llamar_y_retornar("act", "reg", "asignarParqueadero(parqueadero)")
d.cerrar()

d = nuevo("CU-019 Registrar salida de visitante", [actor("Vigilante"), o("reg", "RegistroVisita")])
d.llamar_y_retornar("act", "reg", "estaDentro()", "dentro")
d.abrir("opt", "el visitante está dentro")
d.llamar("act", "reg", "registrarSalida()")
d.propio("reg", "estaDentro()")
d.retornar("reg", "act")
d.cerrar()

d = nuevo("CU-020 Consultar visitantes dentro del conjunto", [actor("Vigilante"), o("reg", "RegistroVisita")])
d.abrir("loop", "por cada visita con ingreso autorizado y sin salida")
d.llamar_y_retornar("act", "reg", "estaDentro()", "true")
d.cerrar()

d = nuevo("CU-021 Consultar historial de accesos",
          [actor("Vigilante o Administrador"), o("reg", "RegistroVisita")])
d.abrir("loop", "por cada visita del periodo y documento filtrados (máximo 500)")
d.llamar_y_retornar("act", "reg", "estaDentro()", "dentro")
d.cerrar()

# ================================================================ Modulo operaciones (CU-022 ... CU-031)
d = nuevo("CU-022 Gestionar correspondencia",
          [actor("Vigilante"), o("per", "Persona"), o("est", "Estado"), o("tra", "TransicionEstado"),
           o("cor", "Correspondencia"), o("cfg", "ConfiguracionNotificacion")])
d.abrir("alt", "registrar la recepción")
d.abrir("opt", "va dirigida a una persona")
d.llamar_y_retornar("act", "per", "vinculoEstaVigente()", "true")
d.cerrar()
d.llamar_y_retornar("act", "est", "obtenerInicial(proceso = CORRESPONDENCIA)", "Pendiente")
d.crear("act", "cor")
d.llamar_y_retornar("act", "cor", "registrarRecepcion(tipo, remitente, descripcion)")
d.abrir("opt", "no va dirigida a una persona (se avisa a las del inmueble)")
d.abrir("loop", "por cada persona del inmueble")
d.llamar_y_retornar("act", "per", "vinculoEstaVigente()", "vigente")
d.cerrar()
d.cerrar()
notificar(d)
d.sino("registrar la entrega")
transicionar(d, "cor")
d.llamar_y_retornar("act", "cor", "registrarEntrega(nombreRecibe, documentoRecibe)")
d.cerrar()

d = nuevo("CU-023 Consultar correspondencia", [actor("Persona vinculada"), o("per", "Persona")])
d.llamar_y_retornar("act", "per", "vinculoEstaVigente()", "vigente")

d = nuevo("CU-024 Gestionar zonas comunes",
          [actor("Administrador"), o("zon", "ZonaComun"), o("hor", "HorarioDisponibilidad")])
d.abrir("alt", "crear una zona común")
d.llamar("act", "zon", "registrar(nombre, descripcion, capacidad, tipoUso, requiereAprobacion)")
d.propio("zon", "modificarDatos(nombre, descripcion, capacidad, tipoUso, requiereAprobacion)")
d.retornar("zon", "act")
d.sino("modificar una zona común")
d.llamar_y_retornar("act", "zon", "modificarDatos(nombre, descripcion, capacidad, tipoUso, requiereAprobacion)")
d.llamar_y_retornar("act", "zon", "limpiarHorarios()")
d.cerrar()
d.llamar_y_retornar("act", "zon",
                    "configurarReglas(duracionMax, antMin, antMax, limiteCancel, maxReservas, periodo)")
d.abrir("loop", "por cada horario (al menos uno)")
d.llamar("act", "zon", "agregarHorario(dia, inicio, fin)")
d.crear("zon", "hor")
d.llamar("zon", "hor", "registrar(dia, inicio, fin)")
d.propio("hor", "modificarHorario(inicio, fin)")
d.retornar("hor", "zon")
d.retornar("zon", "act")
d.cerrar()
d.abrir("alt", "desactivar la zona")
d.llamar_y_retornar("act", "zon", "desactivar(motivo)")
d.sino("reactivar la zona")
d.llamar_y_retornar("act", "zon", "reactivar()")
d.cerrar()

d = nuevo("CU-025 Reservar zona común",
          [actor("Residente"), o("zon", "ZonaComun"), o("hor", "HorarioDisponibilidad"), o("dia", "EDiaSemana"),
           o("man", "Mantenimiento"), o("per", "Persona"), o("est", "Estado"), o("tra", "TransicionEstado"),
           o("res", "Reserva")])
d.llamar("act", "zon", "consultarDisponibilidad(fecha)")
d.llamar_y_retornar("zon", "dia", "desde(dia)", "diaSemana")
d.retornar("zon", "act", "horarios")
d.llamar_y_retornar("act", "per", "vinculoEstaVigente()", "true")
d.llamar_y_retornar("act", "est", "obtenerInicial(proceso = RESERVA)", "Pendiente")
d.crear("act", "res")
d.llamar_y_retornar("act", "res", "registrar(fecha, inicio, fin, asistentes, motivo)")
d.llamar("act", "zon", "validarReserva(reserva)")
d.abrir("opt", "la zona está activa")
d.abrir("loop", "por cada horario, hasta el primero que contiene la franja")
d.llamar("zon", "hor", "contiene(fecha, inicio, fin)")
d.llamar_y_retornar("hor", "dia", "desde(dia)", "diaSemana")
d.retornar("hor", "zon", "contiene")
d.cerrar()
d.cerrar()
d.retornar("zon", "act", "true")
d.abrir("loop", "por cada mantenimiento vigente de la zona")
d.llamar_y_retornar("act", "man", "afectaPeriodo(inicio, fin)", "false")
d.cerrar()
d.abrir("opt", "la zona no requiere aprobación")
transicionar(d, "res")
d.llamar_y_retornar("act", "res", "aprobar(condiciones)")
d.cerrar()

d = nuevo("CU-026 Gestionar reservas propias",
          [actor("Residente"), o("res", "Reserva"), o("est", "Estado"), o("tra", "TransicionEstado")])
d.llamar_y_retornar("act", "res", "puedeCancelarResidente()", "puede")
d.abrir("opt", "faltan más horas que el límite de cancelación de la zona")
transicionar(d, "res")
d.llamar_y_retornar("act", "res", "cancelar(motivo)")
d.cerrar()

d = nuevo("CU-027 Gestionar reservas",
          [actor("Administrador"), o("res", "Reserva"), o("est", "Estado"), o("tra", "TransicionEstado"),
           o("cfg", "ConfiguracionNotificacion")])
transicionar(d, "res")
d.abrir("alt", "aprobar")
d.llamar_y_retornar("act", "res", "aprobar(condiciones)")
d.sino("rechazar")
d.llamar_y_retornar("act", "res", "rechazar(motivo)")
d.sino("anular")
d.llamar_y_retornar("act", "res", "cancelar(motivo)")
d.sino("finalizar")
d.cerrar()
d.abrir("opt", "aprobada, rechazada o anulada")
notificar(d)
d.cerrar()

d = nuevo("CU-028 Gestionar mantenimientos",
          [actor("Administrador"), o("est", "Estado"), o("tra", "TransicionEstado"), o("man", "Mantenimiento")])
d.abrir("alt", "programar un mantenimiento")
d.llamar_y_retornar("act", "est", "obtenerInicial(proceso = MANTENIMIENTO)", "Programado")
d.crear("act", "man")
d.llamar_y_retornar("act", "man", "registrar(tipo, elementoArea, descripcion, inicio, fin, responsable)")
d.sino("modificar (solo Programado)")
d.llamar_y_retornar("act", "man", "modificarDatos(descripcion, inicio, fin, responsable)")
d.sino("iniciar")
transicionar(d, "man")
d.llamar_y_retornar("act", "man", "iniciar()")
d.sino("finalizar")
transicionar(d, "man")
d.llamar_y_retornar("act", "man", "finalizar(observaciones)")
d.abrir("opt", "no tenía fecha y hora de fin")
d.llamar_y_retornar("act", "man", "modificarDatos(descripcion, inicio, fin, responsable)")
d.cerrar()
d.sino("cancelar")
transicionar(d, "man")
d.llamar_y_retornar("act", "man", "cancelar(motivo)")
d.cerrar()

d = nuevo("CU-029 Radicar PQRS",
          [actor("Persona vinculada"), o("per", "Persona"), o("est", "Estado"), o("pqr", "Pqrs")])
d.llamar_y_retornar("act", "per", "vinculoEstaVigente()", "true")
d.llamar_y_retornar("act", "est", "obtenerInicial(proceso = PQRS)", "Radicada")
d.crear("act", "pqr")
d.llamar_y_retornar("act", "pqr", "radicar(tipo, asunto, descripcion)")

d = nuevo("CU-030 Consultar PQRS propias", [actor("Persona vinculada"), o("pqr", "Pqrs")])
d.abrir("loop", "por cada PQRS propia")
d.abrir("opt", "hay plazo de respuesta configurado")
d.llamar_y_retornar("act", "pqr", "estaVencida(plazoDiasHabiles)", "vencida")
d.cerrar()
d.cerrar()

d = nuevo("CU-031 Atender PQRS",
          [actor("Administrador"), o("est", "Estado"), o("tra", "TransicionEstado"), o("pqr", "Pqrs"),
           o("cfg", "ConfiguracionNotificacion")])
d.abrir("alt", "poner en gestión (Radicada → En trámite)")
transicionar(d, "pqr")
d.llamar_y_retornar("act", "pqr", "ponerEnGestion()")
d.sino("responder")
transicionar(d, "pqr")
d.llamar_y_retornar("act", "pqr", "responder(respuesta)")
notificar(d)
d.cerrar()
d.abrir("opt", "hay plazo de respuesta configurado")
d.llamar_y_retornar("act", "pqr", "estaVencida(plazoDiasHabiles)", "vencida")
d.cerrar()


def main():
    """Exporta los SVG con el visor oficial. El XML solo se reescribe con --regenerar-xml (descarta retoques)."""
    if "--regenerar-xml" in sys.argv:
        cuerpo = "".join(d.xml() for d in diagramas)
        with open(SALIDA, "w", encoding="utf-8") as f:
            f.write('<?xml version="1.0" encoding="UTF-8"?>\n<mxfile host="app.diagrams.net">\n' + cuerpo + "</mxfile>\n")
        print(f"{len(diagramas)} diagramas (xml) -> {SALIDA}")
    else:
        print("XML sin tocar (usa --regenerar-xml para reescribirlo; se pierden los retoques hechos en draw.io)")
    if "--sin-svg" not in sys.argv:
        import subprocess
        subprocess.run([sys.executable, os.path.join(AQUI, "exportar_secuencia_svg.py")], check=False)


if __name__ == "__main__":
    main()
