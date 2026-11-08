# CONTEXTO — API Propiedad Horizontal (back-end)

> **Archivo de lectura obligatoria.** Antes de crear, modificar o borrar cualquier cosa del proyecto, lee este
> archivo completo y respeta sus reglas (secciones 5, 6 y 8 en especial). Si una tarea exige romper una regla,
> **detente y consulta al usuario**; si el usuario cambia una regla, **actualiza este archivo en el mismo cambio**.
> No describe cada clase (eso lo dice el código): describe el *por qué* y las *reglas*.

---

## 1. Qué es el sistema

API REST para administrar un conjunto residencial (propiedad horizontal) de la Universidad Simón Bolívar
(`unisimon`). Cubre: administración de personas/inmuebles/vehículos/parqueaderos, control de acceso de visitantes,
operaciones del conjunto (correspondencia, zonas comunes, reservas, mantenimientos, PQRS) y funciones transversales
(sesión, cuentas, catálogos, notificaciones y parámetros).

**Fuentes de diseño** (carpeta local `diagramas_uml/`, ignorada por git, solo referencia):
- `Diagrama_de_clases_v10.drawio.xml` — diagrama de clases. **Fuente de verdad del modelo de datos.** Va comprimido
  (base64 + deflate raw dentro de `<diagram>`); para leerlo hay que descomprimirlo.
- 4 imágenes de casos de uso: Módulo Transversal (CU-001…007), Administración (CU-008…014),
  Seguridad (CU-015…021) y Operaciones (CU-022…031).

Si el diagrama cambia (nueva versión), actualiza entidades **y** la sección 4 de este archivo.

**Documentación de flujos:** el `README.md` muestra un diagrama de secuencia por caso de uso (imágenes SVG en modo claro y sin notas en `docs/diagramas/svg`,
fuentes JSON de Archify en `docs/diagramas/fuente`; los HTML se regeneran y no se versionan). Al cambiar un flujo, actualiza su
fuente `.json`, regenera el HTML y la imagen, y revisa la tabla «Casos de uso y diagramas» del README.

## 2. Stack

| Aspecto | Decisión |
|---|---|
| Lenguaje / build | Java 25, Maven (`./mvnw`), Spring Boot 4.1.x (Spring Security 7, Jackson 3 = `tools.jackson`) |
| Persistencia | Spring Data JPA + Hibernate (`ddl-auto=validate`), PostgreSQL (`compose.yaml`; Boot lo conecta solo en desarrollo). **Esquema con Flyway** (ver sección 9) |
| Seguridad | Spring Security + `oauth2-resource-server` (JWT HS256 con Nimbus). Sin sesión, sin CSRF |
| Correo | Puerto `EnviadorCorreo` con dos adaptadores: SMTP (`spring-boot-starter-mail`) o Gmail API (OAuth2) si hay `GMAIL_REFRESH_TOKEN`; `@Async` para no bloquear la petición |
| Otros | Lombok (`@SuperBuilder`, `@Getter`), Bean Validation |
| Pruebas | JUnit 5 + MockMvc; H2 en memoria (modo PostgreSQL) en `src/test/resources/application.properties` |

## 3. Arquitectura y paquetes (`unisimon.apipropiedadhorizontal`)

```
domain/enums     enums del diagrama (ERolCuenta, ETipoCatalogo, ...)
domain/model     entidades JPA = "mapa" de tablas para el ORM (+ comportamiento propio del dominio)
repository       interfaces Spring Data (una por entidad)
service          interfaces de casos de uso  ──►  service/impl  implementaciones
web/controller   endpoints REST (delgados: validan, delegan, devuelven DTO)
web/dto          records de entrada/salida (Bean Validation)
web/error        ErrorApi (cuerpo de error único) + ManejadorGlobalExcepciones
security         JWT, SecurityConfig, anotaciones de autorización, UsuarioActual
security/anotaciones  etiquetas de autorización (@SoloAdministrador, ...)
exception        RecursoNoEncontrado (404), ReglaNegocio (422), Conflicto (409)
config           AdministradorInicial (bootstrap)
util             utilidades puras (Correos)
```

Flujo: `Controller → Service(interfaz) → Repository → Entidad`. Nunca saltarse capas
(un controlador no usa un repositorio; una entidad no conoce DTOs ni HTTP).

## 4. Dominio

### 4.1 Actores y jerarquía
`Usuario registrado` generaliza a Administrador, Vigilante y **Persona vinculada**; esta generaliza a
**Propietario** y **Residente**. En el modelo:
- `CuentaUsuario.rol` (`ERolCuenta`) = `ADMINISTRADOR | VIGILANTE | PERSONA_VINCULADA`.
- Propietario/Residente **no son roles de cuenta**: se derivan de `Persona.tipoPersona`
  (`esPropietario`/`esResidente`) **con vínculo vigente** (`Persona.vinculoEstaVigente()`), y se calculan al emitir
  el token (`ResolutorAutoridades`). Una persona puede ser ambas cosas.

### 4.2 Entidades → tablas (todas con `id BIGINT` identity vía `EntidadBase`)

| Entidad (tabla) | Notas de mapeo / reglas |
|---|---|
| `CuentaUsuario` (`cuenta_usuario`) | `correo_acceso` único (minúsculas), `hash_contrasena` BCrypt, `persona_id` único y **nullable** (0..1↔0..1). PERSONA_VINCULADA exige persona (regla de servicio) |
| `VerificacionCorreo` (`verificacion_correo`) | `correo` único (minúsculas), `hash_codigo` (SHA-256 del correo + código; **nunca** el código), `expira_en`, `intentos`. Una fila por correo; se borra al confirmarse (ver 4.11) |
| `Persona` (`persona`) | único (`tipo_documento_id`,`numero_documento`); `inmueble_id` nullable (vínculo); `tipo_persona_id` obligatorio |
| `TipoPersona` (`tipo_persona`) | `nombre` único; flags `es_propietario`, `es_residente` |
| `Inmueble` (`inmueble`) | único (`torre_bloque`,`numero`); auto-referencia `inmueble_asociado_id` = inmueble al que pertenece un **parqueadero** (un parqueadero es un Inmueble) |
| `Vehiculo` (`vehiculo`) | `placa` única; persona (responsable) e inmueble obligatorios (composición desde Inmueble) |
| `ValorCatalogo` (`valor_catalogo`) | único (`tipo`,`nombre`); `tipo` = `ETipoCatalogo`. Reemplaza tablas de tipos sueltas |
| `Visitante` (`visitante`) | `numero_documento` único |
| `PreRegistro` (`pre_registro`) | persona+visitante+inmueble obligatorios; `ETipoVigencia`; `estaVigente(fecha)` |
| `RegistroVisita` (`registro_visita`) | pre_registro, residente contactado, parqueadero asignado, tipo_vehiculo **opcionales**; `estaDentro()` |
| `Notificacion` (`notificacion`) | por persona; índice por persona |
| `ConfiguracionNotificacion` (`configuracion_notificacion`) | `evento` único; `generar(...)` crea notificaciones |
| `ConfiguracionGeneral` (`configuracion_general`) | fila única (máx. vehículos por inmueble, plazo PQRS) |
| `Correspondencia` (`correspondencia`) | `persona_id` opcional (si no, va al inmueble); `estado_id` obligatorio |
| `Pqrs` (`pqrs`) | persona+inmueble+tipo+estado obligatorios; `estaVencida(diasHabiles)` (lun–vie, sin festivos) |
| `ZonaComun` (`zona_comun`) | `nombre` único; reglas de reserva; **composición** con `HorarioDisponibilidad` (cascade ALL + orphanRemoval; el UML dice 1..*, mínimo aún no forzado) |
| `HorarioDisponibilidad` (`horario_disponibilidad`) | día (`EDiaSemana`), `hora_inicio`, `hora_fin` |
| `Reserva` (`reserva`) | persona, inmueble, zona, estado obligatorios; índice (zona, fecha); `seTraslapaCon`, `puedeCancelarResidente` |
| `Mantenimiento` (`mantenimiento`) | `zona_comun_id` opcional (0..1); `afectaPeriodo` |
| `Estado` (`estado`) | único (`proceso`,`nombre`); `es_inicial`, `es_final`; máquina de estados **por datos** |
| `TransicionEstado` (`transicion_estado`) | único (origen,destino); `requiere_motivo` |

Tipos: `Date→LocalDate`, `Time→LocalTime`, `DateTime→LocalDateTime`, enums con `@Enumerated(STRING)`.
Todas las asociaciones `ManyToOne`/`OneToOne` son **`LAZY`**; `open-in-view=false`, así que cualquier acceso a
asociaciones debe ocurrir dentro de un `@Transactional` del servicio (nunca en el controlador).

### 4.3 Estados (Correspondencia, Reserva, Mantenimiento, PQRS)
Los estados y transiciones válidas **son datos** (`Estado`/`TransicionEstado`), no `enum`. Las entidades solo
guardan `estado` y exponen `cambiarEstado(...)`; **quién valida la transición es un servicio** (consulta
`TransicionEstadoRepository`) mediante `GestorEstados`. `EstadosIniciales` siembra los estados al arrancar
(idempotente; detalle en `CAMBIOS_DIAGRAMA.md` §3) y los nombres que usa el código están en `NombresEstado`.
Entidad lazy: `EntidadBase.equals` usa `getId()` (no el campo) porque los proxies de Hibernate tienen el campo en null.
Con `@Valid` y `@PreAuthorize` juntos, un body inválido responde 400 antes que el 403 (la validación corre primero).

### 4.4 Casos de uso → quién puede ejecutarlos (etiqueta) y estado

| CU | Descripción | Etiqueta | Estado |
|---|---|---|---|
| 001 | Gestionar sesión (login; cerrar sesión = descartar token) | login público / `@Autenticado` | ✅ `AuthController` |
| 002 | Gestionar contraseña | `@Autenticado` | ✅ cambio (`/api/auth/cambiar-contrasena`) y restablecer por correo (`/api/auth/olvide-contrasena` + `/restablecer-contrasena`, públicos) |
| 003 | Gestionar datos propios | `@SoloPersonaVinculada` | ✅ `PerfilController` |
| 004 | Consultar notificaciones | `@SoloPersonaVinculada` | ✅ `NotificacionController` |
| 005 | Gestionar cuentas de usuario | `@SoloAdministrador` | ✅ `CuentaUsuarioController` |
| 006 | Administrar catálogos | lectura `@Autenticado`, escritura `@SoloAdministrador` | ✅ `CatalogoController` |
| 007 | Configurar parámetros y notificaciones | `@SoloAdministrador` | ✅ `ConfiguracionController` (`/api/configuracion/...`) |
| 008, 009, 010, 012 | Personas (alta con verificación de correo, 4.11), inmuebles, vinculaciones, vehículos | `@SoloAdministrador` | ✅ `PersonaController`, `InmuebleController`, `VehiculoController` |
| 014 | Parqueaderos | `@SoloAdministrador` | ✅ `ParqueaderoController` (`Inmueble.categoria = PARQUEADERO`) |
| 011 | Consultar inmuebles propios | `@SoloPropietario` | ✅ `GET /api/mis-inmuebles` |
| 013 | Consultar vehículo por placa | `@SoloVigilante` | ✅ `GET /api/vehiculos/placa/{placa}` |
| 015 | Gestionar pre-registros de visitantes | `@SoloResidente` | ✅ `PreRegistroController` |
| 016–020 | Autorizar ingreso (17 y 18 son `«extend»` de 16), registrar salida, visitantes dentro | `@SoloVigilante` | ✅ `AccesoController` |
| 021 | Historial de accesos | `@AdministradorOVigilante` | ✅ `GET /api/accesos/historial` |
| 022 | Gestionar correspondencia | `@SoloVigilante` | ✅ `CorrespondenciaController` |
| 023 | Consultar correspondencia | `@SoloPersonaVinculada` | ✅ `GET /api/correspondencia/mias` |
| 024, 027 | Zonas comunes, gestionar reservas | `@SoloAdministrador` | ✅ `ZonaComunController`, `ReservaController` |
| 028 | Mantenimientos | `@SoloAdministrador` | ✅ `MantenimientoController` |
| 031 | Atender PQRS | `@SoloAdministrador` | ✅ `PATCH /api/pqrs/{id}/atender` |
| 025, 026 | Reservar zona común / gestionar reservas propias | `@SoloResidente` | ✅ `ReservaController` |
| 029, 030 | Radicar / consultar PQRS propias | `@SoloPersonaVinculada` | ✅ `PqrsController` |

Los CU "propios" (003, 023, 026, 030, 011…) deben filtrar por `UsuarioActual.personaId()` **en el servicio**:
la etiqueta dice *qué rol*, el servicio garantiza *que solo vea lo suyo* (nunca confiar en un `personaId` del cliente).

### 4.5 Decisiones de implementación (administración)
- **Vínculo persona–inmueble** = campos de `Persona` (`inmueble`, fechas, motivo); una persona tiene un solo vínculo
  vigente. Finalizar el vínculo fija `fechaFinVinculo` = **ayer** (la vigencia acepta fin ≥ hoy). `Inmueble.ocupado`
  se recalcula: hay al menos un residente con vínculo vigente.
- **Fechas de vigencia:** `Persona.vinculoEstaVigente()` usa `LocalDate.now()` (zona del sistema) pero el `Clock` es
  UTC; en servicios que comparen con esa vigencia usa `LocalDate.now(reloj.withZone(ZoneId.systemDefault()))`
  (si no, de noche en Colombia la fecha UTC es «mañana» y el vínculo nace no vigente).
- **Vehículos:** la persona debe tener vínculo vigente con el inmueble; placa en mayúsculas y única; respeta
  `ConfiguracionGeneral.maxVehiculosPorInmueble` (0 o sin fila = sin límite). Baja lógica con motivo.

- **Borrado de personas:** `DELETE /api/personas/{id}` es físico pero **solo sin historia** (`HistorialPersona`: vínculo
  actual o pasado, cuenta, vehículos, correspondencia, PQRS, reservas, pre-registros, notificaciones, visitas); con historia
  da 409 y se usa `desactivar`. Respeta «baja lógica, no borrar datos con historia» (sección 7). Desactivar una persona no
  desactiva su cuenta. `OcupacionInmuebles` recalcula `Inmueble.ocupado` tras vincular, finalizar, desactivar o reactivar.
- **Perfil (CU-003):** `/api/perfil` actúa siempre sobre la persona del token (`PersonaAutenticada`); solo `PERSONA_VINCULADA`.

- **Tipos de persona:** `GET /api/tipos-persona` (`@Autenticado`, solo activos) alimenta el selector del front; el front nunca debe asumir ids.
- **Catálogos iniciales:** `CatalogosIniciales` siembra valores por defecto para los 6 `ETipoCatalogo` (lista en
  `ENDPOINTS.md` del front). Solo crea los que faltan por nombre; no reactiva los que el administrador desactivó.

- **CRUD de inmuebles (CU-009):** `PUT`, `desactivar`/`reactivar` y `DELETE` solo sobre `UNIDAD_PRIVADA` (un parqueadero da 404).
  No se desactiva un inmueble con personas de vínculo vigente (422). `DELETE` es físico solo sin historia (`HistorialInmueble`:
  personas vinculadas actuales o pasadas, parqueaderos asignados, vehículos, correspondencia, PQRS, reservas, pre-registros,
  visitas); con historia 409. Misma lógica que el borrado de personas.

### 4.6 Decisiones de implementación (zonas y reservas)
- **Flujo de reserva:** `Pendiente → Aprobada | Rechazada(motivo) | Cancelada(motivo)`, `Aprobada → Cancelada(motivo) | Finalizada`.
  Si la zona **no** requiere aprobación, la reserva nace `Aprobada`. Los estados finales (Rechazada, Cancelada, Finalizada)
  liberan el horario (el traslape solo mira estados no finales).
- **Reglas de reserva:** las propias de la zona viven en `ZonaComun.validarReserva` (horario, capacidad, duración,
  anticipación); el servicio añade traslape (409) y el máximo por inmueble y periodo (`DIA`/`SEMANA` lunes–domingo/`MES`),
  sin contar rechazadas ni canceladas. El residente cancela hasta `limiteCancelacionHoras` antes; el administrador
  anula sin ese límite. Reserva ajena = 404 (no revela que existe).
- **Zonas:** exigen al menos un horario (UML 1..\*); un `0` en una regla numérica = sin restricción. Lectura de zonas para
  cualquier autenticado; escritura solo administrador. No se bloquea desactivar una zona con reservas futuras.
- **Siembra al arrancar:** `EstadosIniciales` (retira el estado antiguo `Activa` si ninguna reserva lo usa) y
  `TiposPersonaIniciales` (Propietario, Residente, Propietario residente; «persona vinculada» no es un tipo).
- `PersonaAutenticada` entrega la persona del token a los servicios de casos propios (reservas); correspondencia y PQRS
  aún tienen su propia copia de esa lógica (candidatos a refactor).

### 4.7 Decisiones de implementación (mantenimientos)
- **Ciclo:** `Programado → En curso → Completado`; `Programado | En curso → Cancelado(motivo)`. Solo se modifica un
  mantenimiento en su estado inicial (`Programado`). Solo administrador (CU-028).
- **Coherencia con reservas** (`Mantenimiento.zonaComun` 0..1 = «bloquea una zona reservable»): un mantenimiento de zona
  vigente (estado no final) bloquea `POST /api/reservas` en ese horario (409) y aparece en la disponibilidad; y no se
  programa ni se reprograma sobre reservas vigentes (409, hay que anularlas antes). Periodo abierto (`fechaHoraFin` null)
  = sin fin hasta que se finaliza.
- Fechas en hora local del sistema (`LocalDateTime`), como las reservas; inicio en el pasado se rechaza con 5 min de tolerancia.

### 4.8 Decisiones de implementación (seguridad de accesos, notificaciones, propietario)
- **Pre-registro (CU-015):** `FECHA_ESPECIFICA` (hoy o futura, un solo ingreso: se marca `usado`) o `PERMANENTE` (hasta
  cancelar). Solo residente con vínculo vigente; el inmueble sale del vínculo, no del cliente. Duplicados vigentes = 409.
- **Ingreso (CU-016/017):** con pre-registro vigente entra sin contactar al residente; sin pre-registro el vigilante contacta a
  un **residente** vigente del inmueble y registra `APRUEBA` (entra) o `NO_APRUEBA`/`NO_CONTESTA` (se **niega**, pero queda
  registrado en el historial). Un visitante no puede entrar dos veces sin registrar su salida (409). `Visitante` se busca
  por documento y, si existe, se conserva su nombre.
- **Parqueadero de visitante (CU-018):** se asigna solo a quien está dentro, y es un `Inmueble` `PARQUEADERO` activo **sin
  `inmuebleAsociado`** (los asignados a un inmueble no son de visitantes). La ocupación se **deriva** de las visitas dentro:
  no hay estado guardado, y la salida lo libera.
- **Historial (CU-021):** hasta 500 registros recientes, filtros opcionales por día de llegada y documento (`Specification`).
- **Notificaciones (CU-004):** `Notificador` (servicio) las crea en la **misma transacción** del evento, respetando
  `ConfiguracionNotificacion` (sin fila = activa y sin correo; `activa=false` = no genera). Eventos conectados:
  correspondencia recibida, ingreso de visitante, cambios de estado de reserva hechos por el administrador (aprobar, rechazar,
  anular) y respuesta de PQRS. Solo las personas vinculadas tienen notificaciones. El correo se envía si el evento lo configura (4.9, 4.10).
  Los tests que borran personas deben borrar antes `notificaciones` (FK).
- **Inmuebles del propietario (CU-011):** `GET /api/mis-inmuebles` devuelve el inmueble del vínculo vigente si el tipo es
  propietario (lista de 0..1 porque `Persona` tiene un solo vínculo), con sus parqueaderos asignados.
- **Fecha/hora local:** los servicios nuevos usan `RelojLocal` (fecha del sistema, igual que `Persona.vinculoEstaVigente`).
- **No implementado a propósito:** resúmenes/tableros y «actividad reciente» (decisión del usuario; el diagrama no los
  contempla). Administrador y vigilante no tienen perfil de persona: el front usa `/api/auth/me`.

### 4.9 Decisiones de implementación (configuración, CU-007)
- **Parámetros generales** (`ConfiguracionGeneral`, fila única): `GET`/`PUT /api/configuracion/general`. `maxVehiculosPorInmueble`
  (0 = sin límite) lo aplica `VehiculoService.crear`; `plazoRespuestaPqrsDias` (días hábiles, ≥ 1) alimenta `PqrsResponse.vencida`.
  Valores por defecto: 0 y 15 (`ConfiguracionGeneral.porDefecto()`). `ConfiguracionInicial` siembra la fila al arrancar (idempotente)
  y el servicio la crea si faltara.
- **Notificaciones por evento** (`ConfiguracionNotificacion`): `GET /api/configuracion/notificaciones` devuelve siempre los 4
  `ETipoEvento` (uno sin fila = activa y sin correo); `PUT /api/configuracion/notificaciones/{evento}` con `activa` y `enviarCorreo`
  (ambos obligatorios). `activa=false` hace que `Notificador` no genere nada. Con `enviarCorreo=true`, `Notificador`
  envía además un correo a quien tenga correo (ver 4.10).

### 4.10 Decisiones de implementación (correo y restablecer contraseña)
- **Puerto `EnviadorCorreo`** (`service`), con dos adaptadores excluyentes (`@ConditionalOnExpression` sobre `app.mail.gmail.refresh-token`):
  `EnviadorCorreoSmtp` (por defecto) y `EnviadorCorreoGmail` (Gmail API con OAuth2 por HTTPS, puerto 443; se activa con `GMAIL_REFRESH_TOKEN`). Ambos `@Async`, mejor
  esfuerzo: nunca lanzan ni registran el cuerpo del correo. Gmail API existe porque Render gratis **bloquea el SMTP saliente**. Cambia el refresh token (scope `gmail.send`, de la cuenta
  emisora) por un access token que guarda en memoria hasta que vence; nunca registra tokens ni el cuerpo. Sin `MAIL_HOST` ni `GMAIL_REFRESH_TOKEN` la app arranca y solo avisa en el log.
  **Ojo:** si la pantalla de consentimiento de Google queda en *Testing*, el refresh token caduca a los 7 días: pasarla a *In production*. En pruebas lo reemplaza `CorreoCapturado`.
- **Notificaciones por correo:** `NotificadorImpl` envía **después del commit** (si el caso de uso se revierte no sale correo) y solo
  a destinatarios con correo; `Notificacion.enviadaPorCorreo` es verdadero únicamente si de verdad se pidió enviarlo.
- **Restablecer contraseña (CU-002):** `POST /api/auth/olvide-contrasena {correo}` → 204 siempre (no revela si la cuenta existe; solo
  envía si existe y está activa) y `POST /api/auth/restablecer-contrasena {token, nueva}` → 204. Ambos **públicos** (`RutasPublicas`),
  porque quien olvidó su clave no tiene token. El token (`TokenRestablecimientoService`) es un JWT HS256 firmado con la misma clave pero
  **emisor distinto** (`<emisor>-restablecer`): el `JwtDecoder` de la API lo rechaza como token de acceso y el de restablecimiento
  rechaza los de acceso. Vale 30 min (`app.security.jwt.expiracion-restablecimiento`) y es **de un solo uso sin guardar estado**: lleva
  la huella SHA-256 del hash de contraseña vigente y deja de coincidir al cambiarla. Token inválido/expirado/usado → 422.

### 4.11 Decisiones de implementación (verificación de correo al crear persona)
- **Flujo:** `POST /api/personas/verificar-correo {correo}` (`@SoloAdministrador`, 204) genera un código de 6 dígitos (`SecureRandom`),
  guarda su huella y lo envía por `EnviadorCorreo`; `POST /api/personas` con `correo` exige `codigoVerificacion`. La persona se lo dicta
  al administrador. Sin correo no hay verificación. Todo en `VerificacionCorreoService`.
- **Tabla `verificacion_correo` (autorizada por el usuario, excepción a 5.1 solo para esto):** vigencia 10 min, máx. 5 intentos fallidos
  (al agotarlos o vencer se borra la fila y hay que pedir otro código), un código nuevo reemplaza al anterior, el correcto se consume.
  Es un reto de un solo propósito del alta, **no** una sesión: no cambia el carácter stateless de la autenticación.
- **«El correo no existe»:** el SMTP acepta el mensaje aunque la dirección no exista (el rebote llega después), así que no se detecta en
  el momento; lo prueba el código: sin él no se crea la persona (422 «No se pudo verificar el correo…»). Vale cualquier proveedor.
- `PersonaServiceImpl.crear` usa `noRollbackFor = ReglaNegocioException` para que los intentos fallidos se conserven.
- **Pendiente:** `PUT /api/personas/{id}` y `PUT /api/perfil` aún permiten cambiar el correo sin verificarlo.

### 4.12 Decisiones de implementación (borrado, `DELETE`)
- **Todas las entidades creables tienen `DELETE /api/<recurso>/{id}`**, solo `@SoloAdministrador`, 204 al borrar, 404 si no existe.
  Misma política que personas e inmuebles: **borrado físico solo sin historia**; con historia 409 (o 422) y se usa la baja lógica
  (`desactivar`, `cancelar`, `anular`). Los listados no se tocan: no hay columnas nuevas ni migración.
- **Maestros** (la historia = alguna fila que los referencia; `Borrado.fisico` hace `delete` + `flush` y traduce la violación de clave
  foránea a 409): catálogos (`/api/catalogos/{id}`), zonas comunes (se borran con sus horarios; 409 si tienen reservas o mantenimientos),
  parqueaderos (422 si siguen asignados a un inmueble; 409 si tuvieron visitas), vehículos y cuentas (nada depende de ellos; **no se
  puede borrar la cuenta propia**, 422).
- **Registros con máquina de estados** (`Borrado.exigirEstadoInicial`): solo se borran en su **estado inicial**; después 409 («ya tuvo
  movimiento»). Reservas (`Pendiente`; las que nacen `Aprobada` solo se anulan), PQRS (`Radicada`), correspondencia (`Pendiente`) y
  mantenimientos (`Programado`).
- **Pre-registros:** se borran si no tienen visitas registradas (clave foránea); si no, 409 y se usa `cancelar`.
- **Catálogos sembrados:** `CatalogosIniciales` recrea al arrancar los valores iniciales que falten por nombre, así que borrar uno
  sembrado y sin uso lo hace reaparecer en el siguiente arranque (para ocultarlo de verdad, desactivarlo).
- Sin `DELETE` a propósito: visitas/historial de acceso (`RegistroVisita`), visitantes, notificaciones, tipos de persona, estados y
  configuración (son historial o datos de referencia fijos).

## 5. Reglas de seguridad (NO negociables)

1. **Stateless.** Sin `HttpSession`, sin cookies de autenticación, sin CSRF, sin estado de sesión ni lista de
   tokens en servidor/BD. `SessionCreationPolicy.STATELESS`. No introducir cachés de sesión, `@SessionScope`,
   ni tablas de tokens/refresh sin autorización explícita del usuario (única autorizada: `verificacion_correo`, 4.11).
2. **Autenticación = JWT Bearer** (`Authorization: Bearer <token>`), HS256, validado por `JwtDecoder`
   (firma + expiración + emisor). Claims: `sub`=id de cuenta, `correo`, `rol`, `personaId` (si hay),
   `autoridades` (lista con prefijo `ROLE_`), `iss`, `iat`, `exp`, `jti`.
3. **Autorización por ETIQUETA, no por URL.** Los permisos se declaran con anotaciones de
   `security/anotaciones` (meta-anotaciones de `@PreAuthorize`) sobre la clase o el método del controlador:
   `@Autenticado`, `@SoloAdministrador`, `@SoloVigilante`, `@AdministradorOVigilante`, `@SoloPersonaVinculada`,
   `@SoloPropietario`, `@SoloResidente`. **Prohibido** agregar `requestMatchers(...).hasRole(...)` en
   `SecurityConfig`. Para un permiso nuevo, crea una nueva anotación en esa carpeta.
4. **Únicas excepciones por URL** (`RutasPublicas`): `POST /api/auth/login`, `POST /api/auth/olvide-contrasena` y
   `POST /api/auth/restablecer-contrasena`, porque quien las usa aún no tiene token (las dos últimas: autorizadas por el
   usuario al pedir el restablecimiento; ver 4.10). Cualquier otra ruta pública requiere permiso explícito del usuario y registrarse aquí.
5. **Seguro por defecto.** La cadena exige `authenticated()` a todo lo demás; un endpoint sin etiqueta exige al
   menos token válido, y `SeguridadPorEtiquetaTest` **falla** si algún método de controlador no tiene etiqueta
   (salvo `AuthController#iniciarSesion`). Todo endpoint nuevo lleva etiqueta.
6. **Contraseñas** solo como hash BCrypt; mín. 8, máx. 72 caracteres (límite de BCrypt). Nunca se devuelven ni se
   registran en logs. El hash jamás sale en un DTO.
7. **Login sin enumeración de cuentas:** clave mala, cuenta inexistente o inactiva ⇒ el mismo 401
   "Credenciales invalidas".
8. **Secretos fuera del repositorio.** Viven en `.env` (ignorado por git, cargado con `spring.config.import`; las variables de entorno reales tienen prioridad). **Nunca se lee ni se muestra `.env`**; los nombres de variables se documentan en `.env.example`. Reservadas: `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` (OAuth2 futuro). `JWT_SECRET` (≥ 32 caracteres) es obligatorio por entorno; si falta, la
   app no arranca. No poner secretos reales en `application.properties` ni en tests.
9. **Trade-offs conocidos de ser stateless** (aceptados): un token ya emitido sigue válido hasta su `exp`
   aunque se desactive la cuenta o se cambie la contraseña; por eso `app.security.jwt.expiracion` es corta (60 min).
   Los roles Propietario/Residente se congelan en el token hasta el siguiente login.
10. Los servicios obtienen la identidad solo vía la interfaz `UsuarioActual` (no `SecurityContextHolder`).

## 6. Principios SOLID aplicados (y cómo mantenerlos)

- **S — Responsabilidad única:** controlador (HTTP) / servicio (caso de uso) / repositorio (persistencia) /
  entidad (reglas propias del dato). `ResolutorAutoridades` es el único lugar que traduce persona→roles;
  `ManejadorErroresSeguridad` solo formatea 401/403; `AdministradorInicial` solo hace el bootstrap.
- **O — Abierto/cerrado:** roles nuevos = anotación nueva, sin tocar `SecurityConfig`; errores nuevos = excepción
  nueva + un `@ExceptionHandler`; el esquema de estados se extiende con datos, no con `if`/`enum`.
- **L — Sustitución de Liskov:** cualquier implementación de `TokenService`/`UsuarioActual`/`*Service` debe
  cumplir el contrato de su interfaz sin sorpresas (mismas excepciones, mismas garantías).
- **I — Segregación de interfaces:** servicios pequeños por caso de uso (`AutenticacionService`,
  `ContrasenaService`, `CuentaUsuarioService`, `CatalogoService`), no un `UsuarioService` gigante.
- **D — Inversión de dependencias:** controladores y servicios dependen de **interfaces** (`TokenService`,
  `UsuarioActual`, `*Service`), con inyección **por constructor**; `Clock` inyectado para el tiempo en servicios.
  Nada de `new` de colaboradores ni `@Autowired` en campos.

Regla práctica: si una clase mezcla dos razones para cambiar, divídela; si al agregar una variante tocas código
existente con `if/switch` sobre tipo, falta una abstracción.

## 7. Convenciones

- Idioma: **dominio y mensajes en español sin tildes en identificadores**; sufijos técnicos en inglés
  (`Repository`, `Service`, `Controller`, `Request`, `Response`). Mensajes al cliente en español.
- Tablas/columnas en `snake_case` (Boot lo deriva; las tablas se nombran explícitas con `@Table(name=...)`);
  restricciones e índices con nombre (`uk_*`, `fk_*`, `ix_*`).
- Entidades: `extends EntidadBase`, `@SuperBuilder`, constructor sin args `PROTECTED`, **sin setters públicos**:
  el estado cambia con métodos de dominio (`desactivar()`, `cambiarEstado()`…). Booleans por defecto con
  `@Builder.Default`. Con `@SuperBuilder` usar `.<Tipo>map(...)` en streams si el compilador infiere `?`.
- DTO = `record` con Bean Validation. Los controladores usan `@Valid`. Nunca devolver entidades por HTTP.
- Errores: siempre `ErrorApi` (`estado, error, mensaje, ruta, fecha, detalles`). 400 validación/formato,
  401 sin/mal token o credenciales, 403 sin permiso, 404 no existe, 409 duplicado/conflicto, 422 regla de negocio.
- Servicios `@Transactional` (lectura: `readOnly = true`). Los correos se guardan y comparan con
  `Correos.normalizar`. Tiempo: `LocalDateTime.now(clock)` en servicios.
- Baja lógica con `activo/activa` + motivo; no borrar físicamente datos con historia.
- Antes de crear un recurso nuevo (entidad/CU): ¿está en el diagrama? Si no, **pregunta**; no inventes tablas.

## 8. Checklist para añadir funcionalidad

1. Leer este archivo y ubicar el CU en 4.4 (actor ⇒ etiqueta).
2. ¿Necesita datos nuevos? Modificar la entidad **según el diagrama**; repositorio con consultas derivadas.
3. Interfaz de servicio + implementación (`service/impl`), `@Transactional`, validar reglas de negocio ahí.
4. DTOs `record` + controlador delgado **con etiqueta de autorización** en cada método.
5. Casos "propios": filtrar por `UsuarioActual.personaId()` en el servicio.
6. Pruebas: caso feliz + 401 (sin token) + 403 (rol equivocado) + reglas de negocio. `./mvnw test` debe pasar
   (incluye `SeguridadPorEtiquetaTest`).
7. Si cambiaste una entidad: añadir la migración `V<n>__...sql` en `db/migration` (sin ella la app no arranca; ver sección 9).
8. Actualizar la tabla 4.4 (estado ✅) y cualquier decisión nueva en este archivo.

## 9. Operación

Variables de entorno:

| Variable | Obligatoria | Uso |
|---|---|---|
| `JWT_SECRET` | **Sí** (≥ 32 chars) | Clave HS256 |
| `BOOTSTRAP_ADMIN_CORREO` / `BOOTSTRAP_ADMIN_CONTRASENA` | No | Crea el primer administrador al arrancar si no existe ninguno (≥ 8 chars). Con la tabla de cuentas vacía queda con **id 1** (reinicia el contador de identidad); con otras cuentas ya creadas usa el siguiente id. Solo se aplica si no hay administrador: cambiar la clave en el `.env` después no modifica una cuenta existente (usar `/api/auth/cambiar-contrasena`) |
| `MAIL_HOST`, `MAIL_PORT` (587), `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_REMITENTE` | No | SMTP. Sin `MAIL_HOST` no se envían correos (aviso en el log). Gmail: `smtp.gmail.com` + contraseña de aplicación. `MAIL_REMITENTE` vacío usa `MAIL_USERNAME` |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN` | No | Si `GMAIL_REFRESH_TOKEN` tiene valor, el correo sale por la Gmail API en vez de SMTP (necesario en Render gratis). Las dos primeras son del cliente OAuth de Google Cloud; el refresh token se obtiene una vez con la cuenta emisora (scope `https://www.googleapis.com/auth/gmail.send`). `MAIL_REMITENTE` = esa misma cuenta |
| `FRONTEND_URL_RESTABLECER` | No | Página del front a la que apunta el enlace del correo (por defecto `http://localhost:3000/restablecer-contrasena`); en producción, la URL pública del front |

Ejecutar: completa `.env` (plantilla en `.env.example`) y corre `./mvnw spring-boot:run` (necesita Docker para PostgreSQL vía `compose.yaml`).
Pruebas: `./mvnw test` (no necesita Docker ni variables: usa H2 y un secreto de prueba).

Flujo manual: `POST /api/auth/login {correo, contrasena}` → `accessToken`; luego
`Authorization: Bearer <accessToken>`; `GET /api/auth/me` muestra la identidad del token.

### Esquema de base de datos: migraciones con Flyway

El esquema **lo crean y cambian las migraciones** en `src/main/resources/db/migration` (`V1__esquema_inicial.sql`, ...).
Hibernate corre con `ddl-auto=validate` en **todos** los entornos: solo comprueba que las entidades coincidan con la
base. Consecuencias y reglas:

- **Cambiar una entidad (columna, tabla, índice, restricción) exige una migración nueva** `V<n+1>__descripcion.sql`
  en el mismo cambio. Sin ella la app **no arranca** (error de validación de esquema), que es justo el aviso buscado.
- **Nunca editar una migración ya aplicada** (`V1`, `V2`...): Flyway falla por checksum distinto. Se corrige con otra
  migración posterior.
- Nombres: `V<número>__<descripcion_en_snake_case>.sql`, números consecutivos. SQL de PostgreSQL, sin prefijo `public.`.
- **Datos iniciales (catálogos, estados, tipos de persona) NO van en migraciones**: los siembran los `ApplicationRunner`
  de `config/` (idempotentes). Las migraciones son solo esquema.
- `spring.flyway.baseline-on-migrate=true`: una base que ya tenía tablas (creada antes con `ddl-auto=update`) se adopta
  como versión 1 sin recrearse; en una base vacía se aplica `V1` normalmente. `validate` es la red de seguridad si una
  base adoptada no coincide.
- **Pruebas (`./mvnw test`)**: usan H2 con `ddl-auto=create-drop` y `spring.flyway.enabled=false`; las migraciones **no**
  se ejercitan ahí. Verificarlas contra PostgreSQL real: crear una base temporal, arrancar la app apuntando a ella y
  comprobar que `V1..Vn` se aplican y la app inicia (así se verificó `V1`: base vacía y copia de la base de desarrollo,
  esquemas idénticos salvo el *nombre* autogenerado de restricciones `UNIQUE`).
- Aviso: en bases creadas por Hibernate las restricciones `UNIQUE` tienen nombres con hash (`uk...`), distintos de los de
  una base creada con `V1` (`<tabla>_<columna>_key`). Una migración futura que haga `DROP CONSTRAINT` por nombre debe
  consultarlo antes (o usar `IF EXISTS` con ambos nombres).

### Despliegue en Render (pendiente, no implementado)

Flyway ya deja el esquema listo para una base vacía. Falta lo propio de Render: un `Dockerfile` (o buildpack) para el
jar; variables `SPRING_DATASOURCE_URL` (**formato JDBC**: `jdbc:postgresql://host:5432/db`; Render entrega la URL como
`postgres://...`, hay que convertirla), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`
(≥ 32), `BOOTSTRAP_ADMIN_CORREO`/`BOOTSTRAP_ADMIN_CONTRASENA` (solo el primer arranque) y
`SPRING_DOCKER_COMPOSE_ENABLED=false`; revisar CORS/URL del front. Cambiar `JWT_SECRET` por un secreto gestionado.

## 10. Pendientes y decisiones abiertas

- Todos los CU de la tabla 4.4 están implementados (✅); solo queda lo pendiente de esta sección.
- **Página del front `/restablecer-contrasena`**: el correo enlaza a `FRONTEND_URL_RESTABLECER?token=…`; el front debe leer el
  `token` y llamar a `POST /api/auth/restablecer-contrasena` (pendiente del lado del front).
- Correo: SMTP o Gmail API (4.10). El envío real aún no se ha verificado en producción (las pruebas usan un servidor simulado).
- **Login con Google (OAuth2): pendiente, se agrega después** (decisión del usuario). Hoy el `pom.xml` solo trae
  `oauth2-resource-server` (validar el JWT propio); al implementarlo habrá que añadir `spring-boot-starter-security-oauth2-client`,
  usar `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` (ya reservadas en `.env.example`) y respetar las reglas de la sección 5
  (stateless, autorización por etiqueta).
- Dependencias heredadas del proyecto base sin uso hoy: `restclient`, `webclient` (clientes HTTP salientes, para llamar a
  APIs externas; podrían servir para Google/correo) y `restdocs` (+ `asciidoctor-maven-plugin`, para generar documentación
  de la API desde las pruebas). Decisión del usuario pendiente: conservarlas o retirarlas.
- Anotación de las ambigüedades del UML que se resolvieron así: parqueadero = `Inmueble` con `inmuebleAsociado`;
  Propietario/Residente derivados de `TipoPersona`; lectura de catálogos permitida a cualquier autenticado.
