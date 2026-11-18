"""Norma corregida (2026-11-17): en una asociacion unidireccional (atributo simple) solo el extremo destino (x=1) lleva
numero, 1 si NOT NULL y 0..1 si nullable; el extremo origen (x=-1) va sin numero. Las bidireccionales (List en el todo) se
revisan aparte (comparar_relaciones.py)."""
import re, glob, os, html
R = 'src/main/java/unisimon/apipropiedadhorizontal/domain/model/'
s = open('docs/diagramas/Clases/DIAGRAMA DE CLASE.mejorado.xml', encoding='utf-8').read()
def mults(eid):
    r = {}
    for m in re.finditer(r'connectable="0" parent="' + re.escape(eid) + r'"[^>]*value="([^"]*)"[^>]*>\s*<mxGeometry relative="1" x="(-?1)"', s):
        if re.fullmatch(r'[0-9*.]+', m.group(1)): r[m.group(2)] = m.group(1)
    return r
for path in glob.glob(R + '*.java'):
    n = os.path.basename(path)[:-5]; src = open(path, encoding='utf-8').read()
    for m in re.finditer(r'((?:\s*@[^\n]+\n)+)\s*private\s+([\w<>]+)\s+(\w+)\s*[;=]', src):
        ann, t, a = m.groups()
        eid = f'{n}-{a}'
        if f'id="{eid}" edge' not in s: continue
        mm = mults(eid)
        if t.startswith('List'):
            continue
        nullable = not ('nullable = false' in ann or 'optional = false' in ann)
        exp_t = '0..1' if nullable else '1'
        exp_s = None   # el destino no tiene atributo hacia el origen: sin multiplicidad
        flag = '' if (mm.get('1') == exp_t and mm.get('-1') == exp_s) else '  <<<<'
        if flag: print(f'{eid:38s} destino {mm.get("1")} (esp {exp_t})  origen {mm.get("-1")} (esp {exp_s}){flag}')
print('fin')
