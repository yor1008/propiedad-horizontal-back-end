"""Por cada método de service/impl: operaciones del dominio (del diagrama de clases) y ayudantes que invoca, en orden."""
import re, glob, os, sys, importlib.util
here = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('cc', os.path.join(here, 'comparar_clases.py'))
sys.argv = [sys.argv[0], '.'] + sys.argv[1:]
cc = importlib.util.module_from_spec(spec); spec.loader.exec_module(cc)
diag = cc.leer_diagrama()
ops = {o for d in diag.values() for o in d['ops']}
AYUD = {'transicionar', 'notificar', 'recalcular', 'actual', 'obtenerOCrear', 'buscarOCrear', 'enviar'}
filtro = sys.argv[2] if len(sys.argv) > 2 else ''
for p in sorted(glob.glob('src/main/java/unisimon/apipropiedadhorizontal/service/impl/*.java')):
    n = os.path.basename(p)[:-5]
    if filtro and filtro not in n: continue
    src = cc.leer_java(p)
    for m in re.finditer(r'\n    (?:public|private|protected)?\s*(?:static\s+)?[\w<>,\s?]+\s+(\w+)\s*\(([^)]*)\)\s*(?:throws[^{]*)?\{', src):
        name = m.group(1); i = m.end(); depth = 1
        while depth:
            depth += {'{': 1, '}': -1}.get(src[i], 0); i += 1
        body = src[m.end():i]
        calls = [(c.group(1), c.group(2)) for c in re.finditer(r'(\w+(?:\(\))?)\s*\.\s*(\w+)\s*\(', body)]
        calls += [('this', c.group(1)) for c in re.finditer(r'(?<![.\w])(\w+)\s*\(', body)]
        calls.sort(key=lambda x: 0)
        seq = []
        for c in re.finditer(r'(?:(\w+(?:\(\))?)\s*\.\s*)?(\w+)\s*\(', body):
            recv, op = c.group(1), c.group(2)
            if op in ops or op in AYUD or (recv is None and op[0].islower() and op not in ('if','for','while','switch','catch','return','new','super','this','List','Optional','of')):
                seq.append(f'{recv+"." if recv else ""}{op}')
        if seq: print(f'{n}.{name}: ' + ', '.join(seq))
