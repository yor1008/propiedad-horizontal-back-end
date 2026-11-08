# CAMBIOS AL DIAGRAMA — pendientes de reflejar en `Diagrama_de_clases_v10`

> Lista viva de **tablas, atributos, enums, métodos y relaciones** que el código ya usa (o propone) y que el
> diagrama UML aún no tiene o que conviene corregir. Úsalo para actualizar el `.drawio` y luego marca cada ítem.
> **Regla del proyecto:** toda idea nueva de clase, método, atributo o enum se agrega aquí (ver `CLAUDE.md`).
>
> Estado: ⏳ pendiente de pasar al diagrama · ✅ ya está en el diagrama · 💡 propuesta (aún no implementada).

---

## 1. Enums nuevos

| Estado | Enum | Valores | Usado en | Motivo |
|---|---|---|---|---|
| ⏳ | `ECategoriaInmueble` | `UNIDAD_PRIVADA`, `PARQUEADERO` | `Inmueble.categoria` | El UML dice «un parqueadero es un Inmueble» pero no hay forma de distinguirlos (para listar, contar «parqueaderos disponibles» y no vincularles personas). Alternativa descartada: un valor de `TIPO_INMUEBLE` llamado «Parqueadero» (depende de datos que el admin puede renombrar). |

## 2. Atributos nuevos o modificados en clases existentes

| Estado | Clase | Atributo | Tipo | Notas |
|---|---|---|---|---|
| ⏳ | `Inmueble` | `categoria` | `ECategoriaInmueble` | Obligatorio, por defecto `UNIDAD_PRIVADA` (columna con `DEFAULT` para no romper filas previas). |
| ⏳ | `Pqrs` | `adjunto` | `String` (≤ 500), 0..1 | Solo **referencia** (nombre o ruta) a un archivo. La API no guarda el binario; si se quiere subir archivos habrá que decidir almacenamiento (ver 💡 §5). |

## 3. Datos sembrados (no son clases nuevas)

Los estados y transiciones son **datos** de `Estado`/`TransicionEstado` (ya en el diagrama). Se siembran al arrancar
(`EstadosIniciales`), solo si faltan. Conviene dibujarlos como diagramas de estado o como nota en el UML.

| Proceso | Estados (★ inicial, ◼ final) | Transiciones (⚠ = exige motivo) |
|---|---|---|
| `CORRESPONDENCIA` | ★ Pendiente, ◼ Entregada | Pendiente → Entregada |
| `RESERVA` | ★ Pendiente, Aprobada, ◼ Rechazada, ◼ Cancelada, ◼ Finalizada | Pendiente → Aprobada · Pendiente → Rechazada ⚠ · Pendiente → Cancelada ⚠ · Aprobada → Cancelada ⚠ · Aprobada → Finalizada |
| `MANTENIMIENTO` | ★ Programado, En curso, ◼ Completado, ◼ Cancelado | Programado → En curso · En curso → Completado · Programado → Cancelado ⚠ · En curso → Cancelado ⚠ |
| `PQRS` | ★ Radicada, En tramite, ◼ Respondida | Radicada → En tramite · Radicada → Respondida · En tramite → Respondida |

**`TipoPersona` sembrados:** `Propietario` (esPropietario), `Residente` (esResidente) y `Propietario residente` (ambos).
«Persona vinculada» del diagrama de casos de uso es el actor general (rol de cuenta `PERSONA_VINCULADA`), no un tipo.
Conviene dibujarlo como nota en el UML, porque el front lo llamaba «tipo vinculada».

**`ValorCatalogo` sembrados** (datos, no clases nuevas): `TIPO_INMUEBLE` (Apartamento, Casa, Local comercial, Parqueadero),
`TIPO_DOCUMENTO` (Cédula de ciudadanía, Cédula de extranjería, Pasaporte, NIT), `TIPO_VEHICULO` (Carro, Moto, Bicicleta),
`TIPO_CORRESPONDENCIA` (Paquete, Carta, Documento), `TIPO_PQRS` (Petición, Queja, Reclamo, Sugerencia),
`TIPO_MANTENIMIENTO` (Preventivo, Correctivo). Nota: «Parqueadero» como tipo de inmueble convive con
`Inmueble.categoria = PARQUEADERO`; la categoría es la que decide el comportamiento, el tipo es solo una etiqueta.

**Reservas:** el flujo con aprobación sale de `Reserva.aprobar/rechazar/cancelar` y `ZonaComun.requiereAprobacion` del UML;
si la zona no requiere aprobación la reserva nace `Aprobada`. El máximo por inmueble usa `EPeriodoLimite` (DIA, SEMANA, MES).

## 4. Métodos / servicios / relaciones a reflejar

| Estado | Elemento | Descripción |
|---|---|---|
| ⏳ | Servicio `GestorEstados` (`inicial`, `transicionar`, `porNombre`) | Concreta lo que el UML ya insinúa: «quién valida la transición es un servicio». Una sola pieza para los 4 procesos. |
| ⏳ | Relación `Inmueble` —(`inmuebleAsociado`)→ `Inmueble` | Ya existe; precisar en el UML que el origen debe ser `PARQUEADERO` y el destino `UNIDAD_PRIVADA` (regla de servicio). |
| ⏳ | `Persona` ↔ `Inmueble` (vínculo) | El vínculo vive en `Persona` (`inmueble`, `fechaInicioVinculo`, `fechaFinVinculo`, `motivoFinVinculo`): **una sola vinculación vigente** por persona; no hay historial de vínculos anteriores (ver 💡 §5). |
| ⏳ | `Persona.vinculoEstaVigente()` | Usa `LocalDate.now()` del sistema; documentar que `finalizarVinculo` fija «ayer». |
| ⏳ | Parqueadero ↔ `Vehiculo` | Hoy `Vehiculo` solo apunta al inmueble; no hay relación vehículo–parqueadero asignado. |
| ⏳ | `EntidadBase.equals` | Igualdad por `getId()` (segura con proxies lazy). No es del UML, pero conviene anotarlo como nota técnica. |
| ⏳ | `Correspondencia` | Al entregar: `GestorEstados.transicionar(..., "Entregada")` + `registrarEntrega(nombre, documento)`. Si `persona` es null va al inmueble; si no, la persona debe tener vínculo vigente con él. |
| ⏳ | `Pqrs` | Se radica con el inmueble del vínculo vigente de la persona (no lo envía el cliente). `estaVencida` usa `ConfiguracionGeneral.plazoRespuestaPqrsDias`. |
| ⏳ | `Persona.actualizarDatos(nombres, apellidos, telefono, correo)` | **Método nuevo.** El UML solo tiene `actualizarContacto(telefono, correo)`; se agregó para que el administrador y la propia persona corrijan también nombres y apellidos. El documento no se puede cambiar. |
| ⏳ | `Persona` — borrado físico | El UML no tiene operación de eliminar; el `DELETE` solo procede si la persona no tiene historia. Servicios auxiliares: `HistorialPersona.tieneHistorial` y `OcupacionInmuebles.recalcular`. |
| ⏳ | `Persona.desactivar` ↔ `Inmueble.ocupado` | Desactivar o reactivar una persona recalcula la ocupación de su inmueble (hoy el UML no lo dice). |
| ⏳ | `Mantenimiento` ↔ `Reserva` (vía `ZonaComun`) | Regla nueva, no dibujada en el UML: un mantenimiento vigente de una zona bloquea reservas en su horario y no se programa sobre reservas vigentes. La relación `Mantenimiento` —afecta→ `ZonaComun` (0..1) ya existe; falta anotar la restricción. |
| ⏳ | `Mantenimiento.finalizar` | Si el periodo era abierto (`fechaHoraFin` nulo), al finalizar se fija el fin al momento actual. |
| ⏳ | `ConfiguracionNotificacion` sin fila | Convención no dibujada: un evento sin configuración genera notificación activa y sin correo. `generar(destinatarios, mensaje)` se usa desde el servicio `Notificador`, que lo invoca en los 4 `ETipoEvento`. |
| ⏳ | `ConfiguracionGeneral.porDefecto()` y siembra | Método estático nuevo (0 vehículos = sin límite, 15 días hábiles de plazo PQRS). Cumple el CU-007: una única fila, sembrada al arrancar por `ConfiguracionInicial` (que también crea una `ConfiguracionNotificacion` por cada `ETipoEvento`). Servicios nuevos: `ConfiguracionGeneralService` y `ConfiguracionNotificacionService`. |
| ⏳ | `solicitarRestablecimiento` / `restablecerContrasena` | Ya están en el UML; se concretan en `RestablecimientoContrasenaService` + `TokenRestablecimientoService` (JWT de un solo propósito, emisor propio y huella del hash de contraseña para que sea de un solo uso; sin tabla de tokens). |
| ⏳ | Servicio `EnviadorCorreo` | No está en el UML: puerto de salida para enviar correos (adaptadores `EnviadorCorreoSmtp` y `EnviadorCorreoBrevo`, este último por API HTTP para hosts que bloquean SMTP). Lo usan el restablecimiento y `Notificador` cuando `ConfiguracionNotificacion.enviarCorreo` es verdadero; `Notificacion.enviadaPorCorreo` pasa a significar «se envió correo de verdad». |
| ⏳ | `RegistroVisita` ↔ parqueadero de visitante | El parqueadero `parqueaderoAsignado` debe ser `PARQUEADERO` activo **sin** `inmuebleAsociado`; la ocupación se deriva de las visitas dentro (sin atributo nuevo). |
| ⏳ | `RegistroVisita` — ingreso negado | Un intento con `ingresoAutorizado=false` también es un registro (historial); el UML solo muestra `negarIngreso(motivo)`. |
| ⏳ | `PreRegistro.estaVigente` | `FECHA_ESPECIFICA` se marca `usado` tras el primer ingreso; `PERMANENTE` nunca se marca. Servicio auxiliar `Visitantes.obtenerOCrear` (busca por documento). |
| ⏳ | Entidad `VerificacionCorreo` (`correo`, `hashCodigo`, `expiraEn`, `intentos`) y servicio `VerificacionCorreoService` | No está en el UML: código de 6 dígitos que se envía al correo de una persona y se exige en `POST /api/personas` (autorizada por el usuario). `CrearPersonaRequest` gana `codigoVerificacion`. Migración `V2__verificacion_correo.sql`. |
| ⏳ | `Inmueble` — desactivar y borrar | El UML tiene `desactivar(motivo)`/`reactivar()` pero no dice que se prohíbe desactivar con vínculos vigentes (422) ni existe operación de eliminar; el `DELETE` solo procede sin historia (servicio auxiliar `HistorialInmueble.tieneHistorial`). |

## 5. Propuestas 💡 (nada implementado; decide si entran al diagrama)

| Estado | Propuesta | Para qué |
|---|---|---|
| 💡 | Entidad `HistorialVinculo` (persona, inmueble, tipoPersona, fechaInicio, fechaFin, motivo) | Conservar vínculos anteriores; hoy al re-vincular se pisan los datos. Cumpliría «no borrar datos con historia». |
| 💡 | Entidad `Adjunto` (nombre, tipoMime, ruta/almacenamiento, tamaño) relacionada 0..* con `Pqrs` | Reemplazar `Pqrs.adjunto` (String) si se quieren varios archivos o subida real. |
| ⏳ | `ZonaComun` exige mínimo 1 `HorarioDisponibilidad` (ya aplicado en el servicio, como dice el UML 1..*) | Coherencia con el diagrama. |
| 💡 | Atributo `Notificacion.tipo` (enum `ETipoEvento` ya existe) y `Notificacion.referenciaId` | Que el front pinte «Actividad reciente» con tipo y enlace al objeto origen. |
| 💡 | Atributo `Visitante.nombre` en `RegistroVisita`/«Actividad reciente» o entidad `Actividad` | Los tableros piden «últimas actividades» (fecha/hora, título, descripción, tipo); hoy se tendrían que **derivar** uniendo notificaciones, visitas, PQRS, etc. Si resulta caro, crear entidad `Actividad`. |
| 💡 | `Vehiculo` ↔ `Inmueble` (parqueadero asignado) | Saber qué parqueadero usa cada vehículo. |

---

### Cómo mantener este archivo
1. Al agregar una clase/atributo/enum/método al código que **no esté en el diagrama**, añade una fila aquí.
2. Cuando lo dibujes en el `.drawio`, cambia ⏳ → ✅ (o bórralo).
3. Si el cambio altera una regla del sistema, actualiza también `CONTEXTO.md`.
