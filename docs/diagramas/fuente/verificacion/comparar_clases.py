"""Compara clase por clase el diagrama de clases con domain/model + domain/enums (+ repositorios para consultas de clase).

Uso: python comparar_clases.py <raiz-del-repo>
"""
import glob
import html
import os
import re
import sys

RAIZ = sys.argv[1]
XML = os.path.join(RAIZ, 'docs/diagramas/Clases/DIAGRAMA DE CLASE.mejorado.xml')
PKG = os.path.join(RAIZ, 'src/main/java/unisimon/apipropiedadhorizontal')
TIPOS = {'LocalDate': 'Date', 'LocalDateTime': 'DateTime', 'LocalTime': 'Time', 'Integer': 'int', 'Boolean': 'boolean'}


def tipo(t):
    t = re.sub(r'(?:java|jakarta)\.[\w.]*\.(?=[A-Z])', '', t.strip())
    m = re.fullmatch(r'Optional<(.+)>', t)
    if m:
        t = m.group(1)
    for k, v in TIPOS.items():
        t = re.sub(r'\b%s\b' % k, v, t)
    return t.replace(' ', '')


# ------------------------------------------------------------------ diagrama
def leer_diagrama():
    s = open(XML, encoding='utf-8').read()
    diag = {}
    for m in re.finditer(r'<mxCell id="([A-Za-z]+)" parent="1" style="swimlane[^"]*" value="([^"]*)"', s):
        cid, nombre = m.group(1), html.unescape(m.group(2))
        nombre = re.sub(r'<[^>]+>', ' ', nombre).replace('«enumeration»', '').strip().split()[-1]
        d = {'attrs': {}, 'ops': {}, 'lits': set(), 'id': cid}
        for hijo in re.finditer(r'<mxCell id="%s-(\w+)" parent="%s"[^>]*value="([^"]*)"' % (cid, cid), s):
            k, raw = hijo.group(1), html.unescape(hijo.group(2))
            lines = re.split(r'<br\s*/?>|</div>', raw)
            items, cur, curu = [], '', False
            for l in lines:
                u = '<u>' in l
                t = re.sub(r'</?(?:u|b|i|div|span|font|p)\b[^>]*>', '', l)
                t = html.unescape(html.unescape(t)).replace('\xa0', ' ').strip()
                if not t:
                    continue
                if cur and t[0] not in '+-#~':
                    cur += ' ' + t
                    curu = curu or u
                else:
                    if cur:
                        items.append((cur, curu))
                    cur, curu = t, u
            if cur:
                items.append((cur, curu))
            for t, u in items:
                t = re.sub(r'\s+', ' ', t).replace('( ', '(').replace(' )', ')')
                a = re.match(r'([+\-#~])\s*(\w+)\s*:\s*(.+)$', t)
                o = re.match(r'([+\-#~])\s*(\w+)\((.*?)\)\s*(?::\s*(.+))?$', t)
                if o:
                    ps = [tuple(x.strip() for x in p.split(':')) for p in o.group(3).split(',') if p.strip()]
                    ps = [(q[0], q[1].replace(' ', '') if len(q) > 1 else '?') for q in ps]
                    d['ops'][o.group(2)] = (o.group(1), ps, (o.group(4) or 'void').replace(' ', ''), u)
                elif a:
                    d['attrs'][a.group(2)] = (a.group(1), a.group(3).replace(' ', ''))
                else:
                    d['lits'].update(t.split())
        diag[nombre] = d
    return diag


# ------------------------------------------------------------------ código
def leer_java(path):
    src = open(path, encoding='utf-8').read()
    src = re.sub(r'/\*.*?\*/', '', src, flags=re.S)
    return re.sub(r'//[^\n]*', '', src)


def cuerpo_clase(src):
    i = src.index('{', re.search(r'\b(class|enum|interface)\s+\w+', src).end())
    return src[i + 1:src.rindex('}')]


def nivel0(body):
    out, depth = [], 0
    for ch in body:
        if ch == '{':
            depth += 1
            if depth == 1:
                out.append('{')
            continue
        if ch == '}':
            depth -= 1
            if depth == 0:
                out.append('}')
            continue
        if depth == 0:
            out.append(ch)
    return ''.join(out)


VIS = {'public': '+', 'private': '-', 'protected': '#'}


def params(pl):
    ps, depth, cur = [], 0, ''
    for ch in pl:
        if ch == '<':
            depth += 1
        if ch == '>':
            depth -= 1
        if ch == ',' and depth == 0:
            ps.append(cur)
            cur = ''
        else:
            cur += ch
    if cur.strip():
        ps.append(cur)
    res = []
    for p in ps:
        p = re.sub(r'@\w+(\([^)]*\))?', '', p).replace('final ', '').strip()
        t, n = p.rsplit(None, 1)
        res.append((n, tipo(t)))
    return res


MIEMBRO = re.compile(
    r'((?:@[\w.]+(?:\((?:[^()]|\([^()]*\))*\))?\s*)*)'
    r'(public|private|protected)?\s*((?:static|final|abstract|synchronized)\s+)*'
    r'([\w<>,?\[\]]+(?:<[^;(){}=]*>)?)\s+(\w+)\s*(\(([^)]*)\)\s*(?:throws[\w\s,]+)?\{|=[^;]*;|;)')


def leer_codigo():
    code = {}
    for path in glob.glob(PKG + '/domain/model/*.java') + glob.glob(PKG + '/domain/enums/*.java'):
        n = os.path.basename(path)[:-5]
        src = leer_java(path)
        body = nivel0(cuerpo_clase(src))
        c = {'attrs': {}, 'ops': {}, 'lits': set(), 'enum': bool(re.search(r'\benum\s+' + n, src))}
        if c['enum']:
            first = body.split(';')[0]
            c['lits'] = set(x.strip().split('(')[0] for x in first.split(',') if x.strip())
            body = body[len(first):]
        for m in MIEMBRO.finditer(body):
            ann, vis, t, name, rest = m.group(1), m.group(2) or '~', m.group(4), m.group(5), m.group(6)
            if t in ('return', 'new', 'else', 'throw') or name in ('if', 'for', 'while', 'switch'):
                continue
            static = 'static' in (m.group(3) or '') or ' static ' in m.group(0).split(name)[0]
            if rest.startswith('('):
                if '@Override' in ann or name == n:
                    continue
                c['ops'][name] = (VIS.get(vis, '~'), params(m.group(7) or ''), tipo(t), static)
            elif not static:
                c['attrs'][name] = (VIS.get(vis, '~'), tipo(t))
        if 'extends EntidadBase' in src:
            c['attrs'] = {'id': ('-', 'Long'), **c['attrs']}  # heredado; el diagrama lo lista en cada clase
        code[n] = c
    repos = {}
    for path in glob.glob(PKG + '/repository/*.java'):
        src = leer_java(path)
        for m in re.finditer(r'default\s+([\w<>,\s?]+?)\s+(\w+)\s*\(([^)]*)\)', src):
            repos[(os.path.basename(path)[:-5].replace('Repository', ''), m.group(2))] = (
                '+', params(m.group(3)), tipo(m.group(1)), True)
    return code, repos


def main():
    diag = leer_diagrama()
    code, repos = leer_codigo()
    total = 0
    for n in sorted(set(diag) | set(code)):
        if n == 'EntidadBase':
            continue
        out = []
        if n not in code:
            out.append('SOLO EN DIAGRAMA (clase)')
        elif n not in diag:
            out.append('SOLO EN CÓDIGO (clase)')
        else:
            d, c = diag[n], code[n]
            if c['enum']:
                if d['lits'] != c['lits']:
                    out.append('literales: solo diag %s, solo código %s' % (d['lits'] - c['lits'], c['lits'] - d['lits']))
                if c['ops'] or d['ops'] or d['attrs']:
                    out.append('miembros en enum: código %s, diag %s %s' % (list(c['ops']), list(d['ops']), list(d['attrs'])))
            else:
                for a in sorted(set(d['attrs']) | set(c['attrs'])):
                    if a not in c['attrs']:
                        out.append('ATRIBUTO solo en diagrama: %s %s' % (a, d['attrs'][a]))
                    elif a not in d['attrs']:
                        out.append('ATRIBUTO solo en código: %s %s' % (a, c['attrs'][a]))
                    elif d['attrs'][a] != c['attrs'][a]:
                        out.append('ATRIBUTO distinto: %s diag %s código %s' % (a, d['attrs'][a], c['attrs'][a]))
                for o in sorted(set(d['ops']) | set(c['ops'])):
                    dc = d['ops'].get(o)
                    cc = c['ops'].get(o) or repos.get((n, o))
                    if cc is None:
                        out.append('OPERACIÓN solo en diagrama: %s %s' % (o, dc))
                        continue
                    if dc is None:
                        if cc[0] == '+':
                            out.append('OPERACIÓN pública solo en código: %s %s' % (o, cc))
                        continue
                    if dc[0] != cc[0]:
                        out.append('visibilidad %s: diag %s código %s' % (o, dc[0], cc[0]))
                    if dc[1] != cc[1]:
                        out.append('parámetros %s: diag %s código %s' % (o, dc[1], cc[1]))
                    if dc[2] != cc[2]:
                        out.append('retorno %s: diag %s código %s' % (o, dc[2], cc[2]))
                    if dc[3] != cc[3]:
                        out.append('subrayado(static) %s: diag %s código %s' % (o, dc[3], cc[3]))
        if out:
            print(n)
            for x in out:
                print('   ', x)
            total += len(out)
    print('\nTOTAL', total)


if __name__ == '__main__':
    main()
