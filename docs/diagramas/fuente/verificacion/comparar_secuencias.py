"""Comprueba cada mensaje de los diagramas de secuencia contra el diagrama de clases.

- la operacion existe en la clase de la linea de vida destino;
- los argumentos son exactamente los nombres de sus parametros (se acepta 'param = valor', UML 17.4.4.1);
- el retorno: con valor si la operacion devuelve algo, sin etiqueta si es void;
- cobertura: operaciones del diagrama de clases que ningun caso de uso usa.
Uso: python comparar_secuencias.py <raiz-del-repo>
"""
import importlib.util
import os
import re
import sys

RAIZ = os.path.abspath(sys.argv[1])
AQUI = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('cc', os.path.join(AQUI, 'comparar_clases.py'))
sys.argv = [sys.argv[0], RAIZ]
cc = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cc)
diag = cc.leer_diagrama()

GEN = os.path.join(RAIZ, 'docs/diagramas/fuente/secuencia-cu/gen_secuencia_casos_uso.py')
src = open(GEN, encoding='utf-8').read()
gancho = '''
REGISTRO = []
_ll, _ret, _pr, _cr = Diagrama.llamar, Diagrama.retornar, Diagrama.propio, Diagrama.crear
def _llamar(self, a, b, op):
    REGISTRO.append((self.nombre, 'llamada', a, b, op, self.parts[b]['etiqueta']))
    return _ll(self, a, b, op)
def _retornar(self, b, a, valor=None, cerrar=True):
    op = self.ops[b][-1] if self.ops[b] else ''
    REGISTRO.append((self.nombre, 'retorno', b, a, op, valor))
    return _ret(self, b, a, valor, cerrar)
def _propio(self, a, op):
    REGISTRO.append((self.nombre, 'llamada', a, a, op, self.parts[a]['etiqueta']))
    REGISTRO.append((self.nombre, 'propio-sin-retorno', a, a, op, None))
    return _pr(self, a, op)
Diagrama.llamar, Diagrama.retornar, Diagrama.propio = _llamar, _retornar, _propio
diagramas = []
'''
src = src.replace('\ndiagramas = []\n', gancho, 1)
ns = {'__name__': 'secuencias', '__file__': GEN}
sys.path.insert(0, os.path.dirname(GEN))
exec(compile(src, GEN, 'exec'), ns)

usadas = set()
problemas = 0
for nombre, tipo, a, b, op, extra in ns['REGISTRO']:
    if tipo != 'llamada':
        continue
    clase = extra.split(':')[-1]
    m = re.fullmatch(r'(\w+)\((.*)\)', op.strip())
    if not m:
        print(f'{nombre}: no parsea "{op}"'); problemas += 1; continue
    nom, args = m.group(1), [x.strip() for x in m.group(2).split(',') if x.strip()]
    if clase not in diag:
        print(f'{nombre}: {clase} no está en el diagrama de clases'); problemas += 1; continue
    firma = diag[clase]['ops'].get(nom)
    if firma is None:
        print(f'{nombre}: {clase}.{nom} no existe'); problemas += 1; continue
    usadas.add((clase, nom))
    esperados = [p[0] for p in firma[1]]
    dados = [x.split('=')[0].strip() for x in args]
    if dados != esperados:
        print(f'{nombre}: {clase}.{nom}({", ".join(args)}) -> esperado ({", ".join(esperados)})'); problemas += 1
# retornos
for nombre, tipo, b, a, op, valor in ns['REGISTRO']:
    if tipo != 'retorno' or not op:
        continue
    nom = op.split('(')[0]
    clase = None
    for n2, t2, a2, b2, op2, ex2 in ns['REGISTRO']:
        if n2 == nombre and t2 == 'llamada' and b2 == b and op2 == op:
            clase = ex2.split(':')[-1]
    firma = diag.get(clase, {}).get('ops', {}).get(nom)
    if not firma:
        continue
    if firma[2] == 'void' and valor is not None:
        print(f'{nombre}: retorno de {clase}.{nom} con valor "{valor}" pero la operación es void'); problemas += 1
    if firma[2] != 'void' and valor is None:
        print(f'{nombre}: retorno de {clase}.{nom} sin valor pero devuelve {firma[2]}'); problemas += 1
for nombre, tipo, b, a, op, valor in ns['REGISTRO']:
    if tipo == 'propio-sin-retorno':
        clase = None
        firma = None
        for n2, t2, a2, b2, op2, ex2 in ns['REGISTRO']:
            if n2 == nombre and t2 == 'llamada' and a2 == b2 == b and op2 == op:
                firma = diag.get(ex2.split(':')[-1], {}).get('ops', {}).get(op.split('(')[0])
        if firma and firma[2] != 'void':
            print(f'{nombre}: mensaje propio {op} devuelve {firma[2]} (sin flecha de retorno; aceptado en lazos)')
print('\nOperaciones del diagrama de clases que ningún caso de uso usa:')
for clase, d in sorted(diag.items()):
    for nom in d['ops']:
        if (clase, nom) not in usadas:
            print(f'   {clase}.{nom}')
print('\nPROBLEMAS', problemas, '| diagramas', len(ns['diagramas']))
