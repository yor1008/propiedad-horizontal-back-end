"""Cada atributo cuyo tipo es otra clase del diagrama debe tener UNA arista; ninguna arista sin atributo."""
import re, sys, os, importlib.util
here = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('cc', os.path.join(here, 'comparar_clases.py'))
sys.argv = [sys.argv[0], sys.argv[1]]
cc = importlib.util.module_from_spec(spec); spec.loader.exec_module(cc)
code, _ = cc.leer_codigo()
s = open(cc.XML, encoding='utf-8').read()
idn = {}
for m in re.finditer(r'<mxCell id="([A-Za-z]+)" parent="1" style="swimlane[^"]*" value="([^"]*)"', s):
    import html
    idn[m.group(1)] = re.sub(r'<[^>]+>', ' ', html.unescape(m.group(2))).replace('«enumeration»', '').split()[-1]
aristas = []
for m in re.finditer(r'<mxCell id="([^"]+)" edge="1"[^>]*source="([^"]+)"[^>]*style="([^"]*)"[^>]*target="([^"]+)"', s):
    if 'dashed=1' in m.group(3): continue
    aristas.append((m.group(1), idn.get(m.group(2)), idn.get(m.group(4)), 'diamond' in m.group(3)))
usadas = set()
for n, c in sorted(code.items()):
    for a, (v, t) in c['attrs'].items():
        lista = re.fullmatch(r'List<(\w+)>', t)
        dest = lista.group(1) if lista else t
        if dest not in idn.values() or dest == n and not lista and a != 'inmuebleAsociado':
            continue
        cand = [e for e in aristas if e[1] == n and e[2] == dest and e[0].endswith('-' + a)]
        if not cand and lista:   # la colección la representa la arista del todo (rombo)
            cand = [e for e in aristas if e[1] == n and e[2] == dest and e[0].endswith('-' + a)]
        if not cand:   # extremo opuesto de una arista todo-parte
            cand = [e for e in aristas if e[3] and e[1] == dest and e[2] == n]
        if len(cand) != 1: print('SIN ARISTA o DUPLICADA:', n, a, t, cand)
        else: usadas.add(cand[0][0])
for e in aristas:
    if e[0] not in usadas: print('ARISTA sin atributo:', e)
print('ok')
