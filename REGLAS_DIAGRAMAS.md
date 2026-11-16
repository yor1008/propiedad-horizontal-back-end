# Reglas para los diagramas UML

> Copiadas de la skill `diagramas-uml` (`~/.claude/skills/diagramas-uml/`). Los scripts que se nombran abajo
> (`scripts/…`) y las plantillas (`plantillas/gen_*.py`) están en esa carpeta: cópialos a `Diagramas/_fuente/`
> de este proyecto antes de usarlos. Las plantillas traen los datos de HorizontalSoftware y hay que cambiarlos
> por los de este proyecto.


Estas reglas salen del proyecto HorizontalSoftware (propiedad horizontal, Spring Boot). Allí se acordaron con
el usuario una por una. Las razones van al lado de cada regla para que sepas aplicarlas en casos que aquí no
aparecen.

## 0. Reglas generales (valen para todos los diagramas)

1. **Backup ANTES de tocar.** Copia el archivo al lado del original y dentro del repo, con un nombre claro:
   `X.antes-de-<cambio>.xml`. Una copia en una carpeta temporal no sirve. El usuario retoca diagramas a mano en
   draw.io, y una vez se perdieron 30 páginas de secuencia retocadas. Los backups viejos solo se borran cuando
   el usuario confirma que todo quedó bien, y preguntándole primero.
2. **Fiel al código, sin inventar.** Cada caja, atributo, operación, mensaje, nodo o puerto tiene que existir en
   el código o en la infraestructura real. Antes de dibujar, lee el código (modelo, servicios, controladores,
   `Dockerfile`, `docker-compose`, nginx, variables de entorno). Si el usuario dice "al 100%", lo dice literal:
   nombres, tipos de los parámetros y tipo de retorno exactos. Lo que todavía no existe se marca con una nota
   UML ("Pendiente de implementar"), no se dibuja como si funcionara.
3. **Notación OMG UML 2.5.1** (formal/17-12-05, https://www.omg.org/spec/UML/2.5.1/PDF). El PDF pesa unos 18 MB
   y WebFetch no lo baja. Descárgalo con `curl` al scratchpad y extrae páginas con
   `pdftotext -layout -f N -l M`. Cita la cláusula en el docstring del generador. En la sección 7 de este archivo
   están las cláusulas que ya se usaron.
4. **Palabras clave entre comillas angulares** `«device»` (en el XML, `&laquo;…&raquo;`), centradas encima del
   nombre, y el nombre centrado y en **negrita** (9.2.4.1). No inventes estereotipos propios: exigirían un
   Profile (cláusula 12). Usa solo las palabras clave estándar.
5. **Sin subrayados** (`<u>`) en nombres ni miembros. El usuario no los quiere, y la especificación permite
   omitirlos cuando el contexto se entiende.
6. **draw.io como formato.** La fuente es un `.xml` de draw.io. Lo que se ve es un `.drawio.svg` editable (lleva
   el modelo en el atributo `content`), así se ve como imagen en GitHub y se abre en draw.io. No uses archify
   ni Mermaid para estos diagramas: el usuario los quitó.
7. **Generados por script, en `Diagramas/_fuente/`.** Un `gen_<diagrama>.py` por diagrama escribe el XML con
   coordenadas fijas. Así se puede regenerar sin perder el orden. Las plantillas reales están en
   `plantillas/`: cópialas y cambia solo los datos.
   - Si el usuario ya retocó el XML a mano, **no lo regeneres por encima** sin backup y sin avisarle. El
     generador de secuencia solo reescribe el XML con `--regenerar-xml`.
8. **Fondo siempre claro.** draw.io exporta SVG con `light-dark()`, que en modo oscuro se ven negros. Pasa
   `scripts/fondo_claro.py` y deja el hook `scripts/pre-commit` en `scripts/hooks/` con
   `git config core.hooksPath scripts/hooks`.
9. **Verifica dibujándolo.** `python scripts/ver_drawio.py dia.xml out.png` lo dibuja con el visor oficial de
   draw.io (Edge o Chrome headless, necesita internet). Mira el PNG antes de decir que quedó bien: textos
   cortados, flechas sueltas, cajas montadas.
10. **Conectores pegados.** Cada flecha lleva `source`/`target` y puntos fijos (`exitX/exitY`,
    `entryX/entryY`, `exitDx=0…`), así sigue a la caja si el usuario la mueve en draw.io. Nada de flechas
    con `sourcePoint`/`targetPoint` sueltos, salvo dentro de figuras dibujadas (bola y socket de componentes).
11. **Ninguna flecha cruza a otra ni pasa por encima de una caja**: conectores ortogonales, carriles
    propios en los pasillos y hoja todo lo grande que haga falta. Para el método completo usa la skill
    `drawio-diagrama-ordenado`.
12. **Texto escapado dos veces.** draw.io trata `value` como HTML: primero `html.escape(texto)` y después se
    escapa para el atributo XML. Si no, `List<Usuario>` se ve como `List`.
13. **Estructura de carpetas** (la misma en cada proyecto):
    ```
    Diagramas/
      UML/casos-de-uso.drawio.xml  + .drawio.svg
      Clases/<Nombre>.xml          + .drawio.svg
      Secuencia/<caso-de-uso>.svg  (uno por caso de uso; fuente: _fuente/secuencia-casos-de-uso.xml)
      Componentes/componentes-<x>.xml + .svg
      Despliegue/despliegue.xml    + despliegue.drawio.svg
      _fuente/  gen_*.py, conectar_secuencia.py, exportar_*.py, ver_drawio.py, fondo_claro.py
    ```
    El README muestra los `.svg` reales de `Diagramas/`. No dupliques los diagramas en Mermaid.
14. **Al terminar**: si el diagrama cambia el código (p. ej. una firma), corre las pruebas (`mvnw test` o la
    equivalente) antes del commit. Haz commit solo cuando el usuario lo pida, con la identidad git del
    usuario.

## 1. Casos de uso

- Actores con `shape=umlActor` y el nombre debajo. Casos de uso como elipses (`ellipse;shapeInside=1`)
  dentro del rectángulo del sistema (`rounded=1`, título arriba).
- Asociación actor–caso de uso: línea sólida **sin flecha**.
- `«include»`: flecha punteada con punta abierta del caso base al incluido. `«extend»`: flecha punteada con
  punta abierta del caso que extiende al extendido (cláusula 18.1.4, Fig. 18.3). La etiqueta va en la flecha.
- Este diagrama es la **lista maestra**: cada caso de uso tiene exactamente un diagrama de secuencia con el
  mismo nombre (el archivo usa el nombre en kebab-case y sin tildes: `reservar-zona-comun.svg`).
- Lo suele hacer el usuario a mano. Respétalo y solo límpialo (backup antes) si te lo pide.

## 2. Clases

- **Contenido = código del modelo, firma por firma.** Atributos con su tipo, operaciones con nombre, nombre
  y tipo de cada parámetro y tipo de retorno exactamente como en la clase de `model/`. No basta con que la
  operación exista en un servicio con otra firma. Para comprobarlo no busques por nombre: compara clase por
  clase, atributo por atributo y método por método. Las excepciones acordadas con el usuario se anotan.
- Compartimentos: nombre (negrita) / atributos / línea separadora / operaciones. Visibilidad `+ - # ~`.
  Interfaces con `«Interface»` encima del nombre.
- Relaciones (generalización: 9.2.4; asociación, composición y agregación: 11.5.4):
  - herencia: línea sólida con triángulo hueco hacia el padre (`endArrow=block;endFill=0`);
  - realización: punteada con triángulo hueco;
  - composición: rombo lleno en el todo (`startArrow=diamondThin;startFill=1`) → en BD `ON DELETE CASCADE`;
  - agregación: rombo hueco → en BD borrado normal;
  - asociación: línea sólida con multiplicidades en ambos extremos (`1`, `0..*`, `1..*`);
  - dependencia o uso: punteada con punta abierta.
- Cajas del tamaño justo para su texto (ancho entre 230 y 430, alto calculado por líneas). Distribución en
  rejilla con pasillos anchos (GX≈190, GY≈170) por donde van las flechas. Plantilla:
  `plantillas/gen_diagrama_clases.py` (lee el contenido del XML existente y solo recoloca: adapta `FILAS` y
  `rutas`).
- Si el diagrama y el código no coinciden, pregunta qué manda. En HorizontalSoftware mandaba el diagrama: el
  código se cambió para cumplirlo (y se corrieron las pruebas).

## 3. Secuencia: uno por caso de uso

- **Participantes = actor + clases del diagrama de clases** (`:Clase`). **Mensajes = SUS operaciones** con el
  nombre y los parámetros tal como están en el diagrama de clases. Entre todos los diagramas de secuencia se
  cubren todas las operaciones del diagrama de clases. No metas controladores, servicios ni repositorios si
  no están en el diagrama de clases.
- Título de cada página: `Diagrama de secuencia: <caso de uso>`. Sin notas de relleno.
- Notación (17.4.4.1 mensajes, 17.6.4 fragmentos):
  - llamada síncrona: línea sólida con punta **llena** (`endArrow=block;endFill=1`);
  - retorno: línea **punteada** con punta abierta (`dashed=1;endArrow=open;endFill=0`) y el tipo devuelto como
    texto (`List<Usuario>`, `Boolean`). Los tipos genéricos van completos;
  - mensaje a sí mismo: lazo a la derecha con barra de activación desplazada;
  - barras de activación blancas de 10 px de ancho. La punta toca el borde de la barra (±5 px del eje);
  - actor: línea de vida con figura de actor (`shape=umlLifeline;participant=umlActor`), así se le pueden
    pegar flechas;
  - fragmentos `alt` / `opt` / `loop` (`shape=umlFrame`) con la guarda `[condición]` en cursiva, separador de
    ramas punteado, **sin relleno y al frente** (`pointerEvents=0`), así no tapan nada.
- Cada mensaje queda **conectado** a la barra o a la línea de vida que toca: `scripts/conectar_secuencia.py`
  lo hace sin mover nada.
- Una sola fuente `_fuente/secuencia-casos-de-uso.xml` (una página por caso de uso). Cada página se exporta a
  `Secuencia/<caso>.svg` con `scripts/exportar_secuencia_svg.py`, que usa el visor oficial, así sale igual a
  draw.io con los retoques a mano. Plantilla: `plantillas/gen_secuencia_casos_uso.py` (clase `Diagrama` con
  `llamar`, `retornar`, `llamar_y_retornar`, `propio`, `mensaje`, `abrir/sino/cerrar`).
- Casos de falla reales (firma inválida, rechazo, sin cambios) van como ramas `alt`, si el código los maneja.

## 4. Componentes

- Componente: rectángulo con `«component»` y el ícono de componente arriba a la derecha. Externos con
  `«sistema externo»` o `«componente externo»`, base de datos como cilindro.
- Interfaz **provista = bola** (círculo) unida al que la implementa. **Requerida = socket** (semicírculo) al
  final de la línea del que la necesita. El ensamblaje es la bola dentro del socket, con el nombre de la
  interfaz al lado (bola y socket: 10.4.4; componentes: 11.6.4).
- Un recuadro punteado `«subsystem» <Proyecto>` agrupa los componentes internos. Cada componente lista sus
  paquetes reales y unas clases clave. Los números (p. ej. "16 controladores") se cuentan en el código.
- Plantilla: `plantillas/gen_componentes.py` (genera a la vez el SVG dibujado y el XML de draw.io).

## 5. Despliegue

- **Nodo** = cubo 3D (`shape=cube;direction=south`). `«device»` para el hardware o la VM,
  `«executionEnvironment»` para lo que ejecuta (SO de contenedor, Docker, nginx, Node.js, PostgreSQL,
  navegador). Los servicios de terceros (Vercel, Neon, Wompi, Gmail API) son nodos sin palabra clave.
  Anídalos tal como están en la realidad: VM ⊃ Docker ⊃ contenedor ⊃ artefacto.
- **Artefacto** = rectángulo con `«artifact»` y el nombre real del entregable (`demo-0.0.1-SNAPSHOT.jar`,
  el repo del front). Va **anidado** dentro del nodo donde se despliega, y con eso **no hace falta la flecha
  «deploy»** (19.4.4: *"Alternatively, this may be shown by nesting"*).
- **CommunicationPath** = línea **sólida SIN flecha**, igual que una asociación (19.4.4: *"depicted using the
  same as normal Association links"*). Se rotula con protocolo y puerto reales (`HTTPS :443`, `HTTP :8080`,
  `JDBC/TLS :5432`, dominio), no con estereotipos.
- Las únicas flechas posibles en despliegue: `«deploy»` (punteada, del artefacto al nodo, solo si el
  artefacto está **fuera** del nodo) y `«manifest»` (punteada con punta abierta, del artefacto al componente,
  solo si se dibujan componentes). Si el artefacto está anidado y no hay componentes, no hay flechas, y está
  bien.
- Marco con encabezado `dep <Proyecto>` (Anexo A, `shape=umlFrame`). Comentario = nota con la esquina doblada
  unida con línea punteada (7.2.4).
- Ojo con `direction=south`: draw.io gira los puntos de conexión del cubo. El generador los corrige con
  `girar()`. Plantilla: `plantillas/gen_despliegue.py`.

## 6. Exportar y entregar

```
python Diagramas/_fuente/gen_<x>.py
python Diagramas/_fuente/ver_drawio.py Diagramas/<X>/<x>.xml revisar.png      # mirar el PNG
python Diagramas/_fuente/exportar_drawio_svg.py Diagramas/<X>/<x>.xml Diagramas/<X>/<x>.drawio.svg
python Diagramas/_fuente/exportar_secuencia_svg.py                              # secuencias
python Diagramas/_fuente/fondo_claro.py                                         # todos los svg
```

Lista de chequeo antes de entregar:
- [ ] backup hecho antes de cambiar
- [ ] cada elemento existe en el código o la infraestructura (y lo pendiente va como nota)
- [ ] notación revisada contra la cláusula UML correspondiente
- [ ] sin cruces, sin flechas sobre cajas, sin flechas sueltas (visto en el PNG del visor oficial)
- [ ] `.drawio.svg` exportado, fondo claro, README apuntando a él
- [ ] pruebas en verde si se tocó código; commit solo si el usuario lo pide

---

## 7. Notación OMG UML 2.5.1 usada (formal/17-12-05)

Las citas son textuales del PDF. "Pág." es el número impreso. La página del PDF es unas 42 más
(p. ej. la impresa 658 es la ~700 del PDF).

Cómo leerlo: el PDF pesa unos 18 MB y WebFetch no lo baja.
```
curl -sL -o <scratchpad>/uml/uml.pdf https://www.omg.org/spec/UML/2.5.1/PDF
pdftotext -layout -f 680 -l 720 <scratchpad>/uml/uml.pdf salida.txt     # rango de páginas del PDF
```
(Extraer el PDF completo tarda más de 2 minutos: hazlo en segundo plano o solo el rango que necesites.)

| Cláusula | Pág. | Qué fija |
|---|---|---|
| 7.2.4 Comment | 22 | Nota: rectángulo con la esquina doblada, unido con línea punteada al elemento que comenta |
| 9.2.4 Classifier | 101 | Palabra clave «…» centrada encima del nombre; nombre centrado y en negrita. *"A Generalization is shown as a line with a hollow triangle as an arrowhead"* |
| 10.4.4 Interface | — | Interfaz provista = *"circle or ball, often also called lollipop"*; requerida (Usage) = *"half-circle or socket"*; ensamblaje = socket que abraza la bola |
| 11.5.4 Association | 201 | Asociación = línea sólida con multiplicidades; composición = rombo lleno en el todo; agregación = rombo hueco |
| 11.6.4 Component | 210 | Componente = rectángulo con «component» o con el ícono de componente |
| 17.4.4.1 Message | 576 | *"A synchronous Message … has a filled arrow head"*; *"A reply Message … has a dashed line with either an open or filled arrow head"*; asíncrono = punta abierta; creación = punteada con punta abierta |
| 17.6.4 CombinedFragment | 585 | Marco con el operador (alt/opt/loop) en la esquina; guardas [cond]; ramas separadas con línea punteada |
| 18.1.4 Use cases | 641 | Include y Extend: *"dashed arrow with an open arrowhead"* con «include» / «extend»; Extend apunta del que extiende al extendido; Include del base al incluido |
| 19.2.4 Deployment | ~656 | Deployment = dependencia punteada con «deploy», del artefacto al nodo; sin etiqueta si está dentro del nodo |
| 19.3.4 Artifact | ~657 | Rectángulo con «artifact»; Manifestation = *"dashed line with an open arrow-head, labeled with the keyword «manifest»"* |
| 19.4.4 Node | 658 | Nodo = cubo; «device», «executionEnvironment»; *"CommunicationPaths between Nodes are depicted using the same as normal Association links"* (sólida, sin flecha); el «deploy» se puede reemplazar *"by nesting DeployedArtifact graphics inside Node symbols"* |
| 12 Profiles | — | Un estereotipo propio exige un Profile: no inventes «…» fuera de los estándar |
| Anexo A | 684 | Marco del diagrama con encabezado `<kind> <name>`. Formas cortas: `dep` despliegue, `sd` secuencia, `cmp` componentes, `uc` casos de uso, `pkg` paquetes, `act` actividad, `stm` máquina de estados; clases solo tiene forma larga: `class` |

Figuras útiles: 18.3 (include/extend), 19.12 (device con executionEnvironment y CommunicationPath),
19.15 (CommunicationPath entre AppServer con artefactos y DBServer, pág. del PDF 701).
