"""Aplica las reglas de conexiones (Manuel, 2026-11-19) al diagrama de clases de Yor sin tocar las cajas.
- estilo base + tipo (asociacion / agregacion / composicion)
- anclas fijas con 4 decimales y Dx/Dy=0, distintas por lado de cada caja
- etiquetas hijas: multiplicidad en el extremo que cuenta y rol (atributo del source) junto al destino
Uso: python reglas_conexiones.py diagrama.xml
"""
import sys, re, xml.etree.ElementTree as ET
from collections import defaultdict

ruta = sys.argv[1]
arbol = ET.parse(ruta)
raiz = arbol.getroot()
modelo = raiz.find(".//root")
celdas = list(modelo)
por_id = {c.get("id"): c for c in celdas}
cajas = {}
for c in celdas:
    if c.get("vertex") == "1" and c.get("parent") == "1":
        g = c.find("mxGeometry")
        cajas[c.get("id")] = tuple(float(g.get(k)) for k in ("x", "y", "width", "height"))

BASE = ("edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;"
        "strokeColor=#000000;strokeWidth=1;jumpStyle=arc;jumpSize=8;")
TIPO = {
    "asociacion": "startArrow=none;endArrow=open;endFill=0;endSize=10;",
    "agregacion": "startArrow=diamondThin;startFill=0;startSize=16;endArrow=none;",
    "composicion": "startArrow=diamondThin;startFill=1;startSize=16;endArrow=none;",
}
ETQ = ("edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];labelBackgroundColor=none;"
       "fontFamily=Courier New;fontSize=11;fontColor=#000000;")
# multiplicidad del extremo source (solo cuando la parte tiene atributo hacia el todo)
MULT_SOURCE = {"Inmueble-personas": "0..1", "Inmueble-parqueaderos": "0..1",
               "ZonaComun-horarios": "1", "Estado-transiciones": "1"}

aristas = [c for c in celdas if c.get("edge") == "1"]
info = {}
for e in aristas:
    est = dict(p.split("=", 1) for p in e.get("style").split(";") if "=" in p)
    if est.get("startArrow") == "diamondThin":
        tipo = "composicion" if est.get("startFill") == "1" else "agregacion"
    else:
        tipo = "asociacion"
    hijos = [k for k in celdas if k.get("parent") == e.get("id")]
    nums = [k.get("value") for k in hijos if re.fullmatch(r"[0-9.*]+", k.get("value") or "")
            and float(k.find("mxGeometry").get("x")) > 0]
    assert len(nums) == 1, (e.get("id"), nums)
    rol = e.get("id").split("-", 1)[1]
    a = {k: float(est[k]) for k in ("exitX", "exitY", "entryX", "entryY")}
    info[e.get("id")] = dict(celda=e, tipo=tipo, mult=nums[0], rol=rol, anc=a, hijos=hijos)


def lado(fx, fy):
    if fy == 0: return "arriba"
    if fy == 1: return "abajo"
    if fx == 0: return "izquierda"
    if fx == 1: return "derecha"
    raise ValueError((fx, fy))


# --- anclas distintas por lado de caja ---
uso = defaultdict(list)  # (caja, lado) -> [(frac, arista, extremo)]
for i, d in info.items():
    e = d["celda"]
    for ext, caja in (("exit", e.get("source")), ("entry", e.get("target"))):
        fx, fy = d["anc"][ext + "X"], d["anc"][ext + "Y"]
        l = lado(fx, fy)
        frac = fx if l in ("arriba", "abajo") else fy
        uso[(caja, l)].append((round(frac, 4), i, ext))
for k, v in uso.items():
    fr = [f for f, _, _ in v]
    if len(set(fr)) != len(fr):
        print("ANCLA REPETIDA", k, v)

# --- reescribir estilo ---
for i, d in info.items():
    a = d["anc"]
    anc = (f"exitX={a['exitX']:.4f};exitY={a['exitY']:.4f};exitDx=0;exitDy=0;"
           f"entryX={a['entryX']:.4f};entryY={a['entryY']:.4f};entryDx=0;entryDy=0;")
    anc = re.sub(r"=(\d)\.0000;", r"=\1;", anc)
    d["celda"].set("style", BASE + TIPO[d["tipo"]] + anc)

# --- orden de llegada por lado (para repartir etiquetas) ---
def pos_abs(i, ext):
    d = info[i]; e = d["celda"]
    caja = e.get("source") if ext == "exit" else e.get("target")
    x, y, w, h = cajas[caja]
    return x + d["anc"][ext + "X"] * w, y + d["anc"][ext + "Y"] * h

def ancho(t):
    return round(3.5 * len(t) + 10.8, 1)

def offset(l, texto, haciafuera):
    """haciafuera: -1 = izquierda/arriba de la linea, +1 = derecha/abajo (posicion por defecto segun la norma)."""
    a = ancho(texto)
    if l == "arriba": return (-a, -16)
    if l == "abajo": return (a, 16)
    if l == "izquierda": return (-a + 1, 15)
    return (a - 1, -15)

def colocar(l, texto, lado_linea, fila):
    """lado_linea: -1 izq/arriba de la linea, +1 der/abajo. fila 0 = pegada al borde, 1 = segunda linea."""
    a = ancho(texto)
    if l in ("arriba", "abajo"):
        dy = (-16 - 14 * fila) if l == "arriba" else (16 + 14 * fila)
        return (lado_linea * a, dy)
    dx = (-a + 1) if l == "izquierda" else (a - 1)
    if l == "izquierda": dx -= 0
    return (dx, lado_linea * (15 + 14 * fila))

# extremos de cada lado ordenados por posicion: el primero manda sus etiquetas a la izq/arriba, el ultimo a la der/abajo
extremos_lado = defaultdict(list)
for i, d in info.items():
    e = d["celda"]
    for ext, caja in (("exit", e.get("source")), ("entry", e.get("target"))):
        l = lado(d["anc"][ext + "X"], d["anc"][ext + "Y"])
        extremos_lado[(caja, l)].append((i, ext))

def etiquetas_extremo(i, ext):
    d = info[i]
    if ext == "entry":
        return [d["mult"], d["rol"]]
    m = MULT_SOURCE.get(i)
    return [m] if m else []

# --- reconstruir etiquetas ---
for i, d in info.items():
    e = d["celda"]
    for k in d["hijos"]:
        modelo.remove(k)
    n = 0
    for ext, xlab in (("exit", "-1"), ("entry", "1")):
        textos = etiquetas_extremo(i, ext)
        if not textos:
            continue
        caja = e.get("source") if ext == "exit" else e.get("target")
        l = lado(d["anc"][ext + "X"], d["anc"][ext + "Y"])
        grupo = extremos_lado[(caja, l)]
        # solo cuentan los extremos con etiquetas
        grupo = [g for g in grupo if etiquetas_extremo(*g)]
        eje = 0 if l in ("arriba", "abajo") else 1
        grupo.sort(key=lambda g: pos_abs(*g)[eje])
        idx = grupo.index((i, ext))
        defecto = -1 if l in ("arriba", "izquierda") else 1  # lado de la multiplicidad por norma
        if l in ("izquierda", "derecha"):
            defecto = 1 if l == "izquierda" else -1
        if len(grupo) > 1 and idx == 0:
            lados = [-1] * len(textos)          # todas hacia la izquierda / arriba
        elif len(grupo) > 1 and idx == len(grupo) - 1:
            lados = [1] * len(textos)           # todas hacia la derecha / abajo
        else:
            lados = [defecto, -defecto][:len(textos)]
        filas = {}
        for t, ll in zip(textos, lados):
            fila = filas.get(ll, 0); filas[ll] = fila + 1
            ox, oy = colocar(l, t, ll, fila)
            n += 1
            lab = ET.Element("mxCell", {"id": f"{i}-etq{n}", "value": t, "style": ETQ, "connectable": "0",
                                        "parent": i, "vertex": "1"})
            g = ET.SubElement(lab, "mxGeometry", {"x": xlab, "relative": "1", "as": "geometry"})
            ET.SubElement(g, "mxPoint", {"x": f"{ox:g}", "y": f"{oy:g}", "as": "offset"})
            modelo.insert(list(modelo).index(e) + n, lab)

ET.indent(arbol, space="  ")
arbol.write(ruta, encoding="utf-8", xml_declaration=False)
ids = [c.get("id") for c in raiz.iter("mxCell")]
assert len(ids) == len(set(ids)), "ids duplicados"
print("ok", len(info), "aristas")
