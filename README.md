# API de Propiedad Horizontal

API REST para administrar un conjunto residencial: personas, inmuebles, vehículos y parqueaderos; control de acceso de visitantes; correspondencia, zonas comunes, reservas, mantenimientos y PQRS; y las funciones transversales (sesión, cuentas, catálogos, notificaciones y parámetros). Proyecto de la Universidad Simón Bolívar.

| | |
|---|---|
| Lenguaje y build | Java 25, Maven (`./mvnw`), Spring Boot 4.1 |
| Persistencia | Spring Data JPA + PostgreSQL; esquema versionado con **Flyway** (`src/main/resources/db/migration`) |
| Seguridad | Spring Security + JWT HS256 **stateless**; autorización por etiqueta en cada endpoint |
| Correo | Puerto `EnviadorCorreo` con dos adaptadores: SMTP o Gmail API (OAuth2) |
| Pruebas | JUnit 5 + MockMvc con H2 en memoria (modo PostgreSQL) |
| Entrega | `Dockerfile` + GitHub Actions (pruebas y build de la imagen) |

El contrato de endpoints (rutas, DTOs y códigos de error) vive en el repositorio del front, en `ENDPOINTS.md`.

## Contenido

1. [Inicio rápido](#inicio-rápido)
2. [Arquitectura](#arquitectura) (incluye los diagramas de [componentes](#diagrama-de-componentes) y de [paquetes](#diagrama-de-paquetes))
3. [Seguridad](#seguridad)
4. [Casos de uso](#casos-de-uso)
5. [Diagramas de secuencia](#diagramas-de-secuencia)
6. [Variables de entorno](#variables-de-entorno)
7. [Correo con la Gmail API](#correo-con-la-gmail-api)
8. [Despliegue](#despliegue)
9. [Cómo regenerar los diagramas](#cómo-regenerar-los-diagramas)

## Inicio rápido

```bash
# Pruebas: no necesitan Docker ni variables (usan H2 y un secreto de prueba)
./mvnw test

# Desarrollo: copia .env.example a .env, complétalo y arranca.
# compose.yaml levanta PostgreSQL con Docker.
./mvnw spring-boot:run
```

Para probarla a mano:

1. `POST /api/auth/login` con `{ "correo": "...", "contrasena": "..." }` devuelve un `accessToken`.
2. Envía `Authorization: Bearer <accessToken>` en las demás peticiones.
3. `GET /api/auth/me` muestra la identidad contenida en el token.

El primer administrador se crea al arrancar si defines `BOOTSTRAP_ADMIN_CORREO` y `BOOTSTRAP_ADMIN_CONTRASENA` y aún no existe ninguno.

## Arquitectura

### Diagrama de componentes

Diagrama de componentes UML con conectores de ensamblaje (bola = interfaz provista, socket = interfaz requerida). Muestra cómo se conectan los componentes: el front consume la API REST, los filtros JWT entregan la petición autenticada a los controladores, estos usan las interfaces `*Service`, los servicios usan `*Repository`, `TokenService`, `Notificador` y `EnviadorCorreo`, y los repositorios y Flyway usan JDBC contra PostgreSQL.

![Diagrama de componentes](docs/diagramas/svg/29-componentes.svg)

Fuente editable: [`docs/diagramas/fuente/29-componentes.drawio.xml`](docs/diagramas/fuente/29-componentes.drawio.xml). Los `.drawio.xml` se abren en [draw.io](https://app.diagrams.net); después de editarlos, exporta de nuevo el SVG con **Archivo → Exportar como → SVG**.

### Diagrama de paquetes

Muestra los paquetes Java (`unisimon.apipropiedadhorizontal`) y qué paquete usa a cuál: la flecha punteada `- - ->` significa "importa clases de". `service.impl` implementa las interfaces de `service` (flecha con triángulo hueco). Las capas solo se conocen por interfaces: `web.controller` depende de `service` (no de `service.impl`) y `service.impl` depende de las interfaces de `repository`. `domain` va abajo como base porque casi todos los paquetes lo usan; para no saturar el diagrama solo se dibuja la flecha desde `repository`. Las dependencias salen de los `import` del código.

![Diagrama de paquetes](docs/diagramas/svg/30-paquetes.svg)

Fuente editable: [`docs/diagramas/fuente/30-paquetes.drawio.xml`](docs/diagramas/fuente/30-paquetes.drawio.xml).

### Capas

```
web/controller   endpoints REST delgados: validan, delegan y devuelven DTO
web/dto          records de entrada y salida (Bean Validation)
web/error        cuerpo de error único y manejador global de excepciones
service          interfaces de casos de uso  →  service/impl  implementaciones
repository       interfaces Spring Data (una por entidad)
domain/model     entidades JPA con su comportamiento propio
domain/enums     enumeraciones del dominio
security         JWT, SecurityConfig, etiquetas de autorización, UsuarioActual
exception        RecursoNoEncontrado (404), ReglaNegocio (422), Conflicto (409)
config           siembras al arrancar (estados, catálogos, administrador inicial)
```

El flujo es siempre `Controller → Service (interfaz) → Repository → Entidad`, sin saltarse capas: los controladores nunca usan repositorios y las entidades no conocen DTOs ni HTTP.

Códigos de error: **400** validación, **401** sin token o credenciales inválidas, **403** sin permiso, **404** no existe, **409** duplicado o con historia, **422** regla de negocio.

## Seguridad

- **Sin estado:** no hay sesiones en el servidor; cada petición se valida solo con el JWT. Los tokens de restablecimiento de contraseña usan un emisor distinto, así que no sirven como tokens de acceso.
- **Autorización por etiqueta, no por URL:** cada método de controlador lleva una anotación (`@SoloAdministrador`, `@SoloResidente`, `@SoloVigilante`, `@SoloPropietario`, `@SoloPersonaVinculada`, `@AdministradorOVigilante`, `@Autenticado`). La prueba `SeguridadPorEtiquetaTest` falla si algún endpoint queda sin etiqueta.
- **Secretos fuera del repositorio:** van en `.env` (ignorado por git) o en las variables del servicio. `.env.example` solo documenta los nombres.

## Casos de uso


| CU | Descripción | Etiqueta | Diagrama |
|---|---|---|---|
| 001 | Gestionar sesión | login público | [Login](#cu-001-iniciar-sesión) |
| 002 | Gestionar contraseña | `@Autenticado` / público | [Cambiar](#cu-002-cambiar-contraseña), [restablecer](#cu-002-restablecer-contraseña-por-correo) |
| 003 | Datos propios | `@SoloPersonaVinculada` | [Perfil](#cu-003-y-cu-011-datos-propios-y-mis-inmuebles) |
| 004 | Notificaciones | `@SoloPersonaVinculada` | [Generar](#cu-004-generar-notificaciones-y-correo-por-evento), [consultar](#cu-004-consultar-notificaciones-propias) |
| 005 | Cuentas de usuario | `@SoloAdministrador` | [Cuentas](#cu-005-cuentas-de-usuario) |
| 006 | Catálogos | lectura `@Autenticado`, escritura admin | [Catálogos](#cu-006-catálogos) |
| 007 | Parámetros y notificaciones | `@SoloAdministrador` | [Configuración](#cu-007-configuración) |
| 008 | Personas | `@SoloAdministrador` | [Alta](#cu-008-alta-de-persona-con-verificación-de-correo), [gestión](#cu-008-editar-desactivar-y-borrar-personas) |
| 009, 010 | Inmuebles y vínculos | `@SoloAdministrador` | [Inmuebles](#cu-009-y-cu-010-inmuebles-y-vínculos) |
| 011 | Inmuebles propios | `@SoloPropietario` | [Mis inmuebles](#cu-003-y-cu-011-datos-propios-y-mis-inmuebles) |
| 012, 013 | Vehículos y consulta por placa | admin / `@SoloVigilante` | [Vehículos](#cu-012-y-cu-013-vehículos-y-consulta-por-placa) |
| 014 | Parqueaderos | `@SoloAdministrador` | [Parqueaderos](#cu-014-parqueaderos) |
| 015 | Pre-registros | `@SoloResidente` | [Pre-registro](#cu-015-pre-registro-de-visitantes) |
| 016, 017 | Ingreso de visitantes | `@SoloVigilante` | [Con pre-registro](#cu-016-ingreso-con-pre-registro), [sin pre-registro](#cu-016-y-cu-017-ingreso-sin-pre-registro) |
| 018, 019, 020 | Parqueadero, salida y visitantes dentro | `@SoloVigilante` | [Parqueadero y salida](#cu-018-cu-019-y-cu-020-parqueadero-de-visitante-y-salida) |
| 021 | Historial de accesos | `@AdministradorOVigilante` | [Historial](#cu-021-historial-de-accesos) |
| 022, 023 | Correspondencia | vigilante / persona vinculada | [Correspondencia](#cu-022-y-cu-023-correspondencia) |
| 024 | Zonas comunes | `@SoloAdministrador` | [Zonas](#cu-024-zonas-comunes) |
| 025, 026 | Reservar y gestionar las propias | `@SoloResidente` | [Reservar](#cu-025-y-cu-026-reservar-una-zona-común) |
| 027 | Gestionar reservas | `@SoloAdministrador` | [Gestión](#cu-027-gestionar-reservas) |
| 028 | Mantenimientos | `@SoloAdministrador` | [Mantenimientos](#cu-028-mantenimientos) |
| 029, 030 | Radicar y consultar PQRS | `@SoloPersonaVinculada` | [PQRS](#cu-029-cu-030-y-cu-031-pqrs) |
| 031 | Atender PQRS | `@SoloAdministrador` | [PQRS](#cu-029-cu-030-y-cu-031-pqrs) |

Además, todas las entidades creables tienen `DELETE` (solo administrador): se borra de verdad solo si no tiene historia; si la tiene responde 409 y se usa `desactivar`, `cancelar` o `anular`. Ver [Borrado](#borrado-físico-solo-sin-historia).


## Diagramas de secuencia

Cada diagrama muestra quién llama a quién y en qué orden. Las flechas verdes son la solicitud principal; las rojas discontinuas, llamadas de seguridad; y las grises discontinuas, respuestas. Son SVG en modo claro: haz clic en uno para verlo a tamaño completo.

### Arranque y seguridad

#### Arranque de la aplicación
![Arranque de la aplicación](docs/diagramas/svg/00-arranque.svg)

#### CU-001 Iniciar sesión
![CU-001 Iniciar sesión](docs/diagramas/svg/01-login.svg)

#### Petición autenticada y autorización por etiqueta
![Petición autenticada](docs/diagramas/svg/02-peticion-autenticada.svg)

#### CU-002 Cambiar contraseña
![CU-002 Cambiar contraseña](docs/diagramas/svg/03-cambiar-contrasena.svg)

#### CU-002 Restablecer contraseña por correo
![CU-002 Restablecer contraseña](docs/diagramas/svg/04-restablecer-contrasena.svg)

### Notificaciones y perfil

#### CU-004 Generar notificaciones y correo por evento
![Notificaciones por evento](docs/diagramas/svg/05-notificaciones-evento.svg)

#### CU-004 Consultar notificaciones propias
![Consultar notificaciones](docs/diagramas/svg/06-notificaciones-consulta.svg)

#### CU-003 y CU-011 Datos propios y mis inmuebles
![Perfil y mis inmuebles](docs/diagramas/svg/07-perfil-y-mis-inmuebles.svg)

### Administración

#### CU-008 Alta de persona con verificación de correo
![Alta de persona](docs/diagramas/svg/08-alta-persona-verificacion.svg)

#### CU-008 Editar, desactivar y borrar personas
![Gestión de personas](docs/diagramas/svg/09-personas-gestion.svg)

#### CU-009 y CU-010 Inmuebles y vínculos
![Inmuebles y vínculos](docs/diagramas/svg/10-inmuebles-y-vinculos.svg)

#### CU-012 y CU-013 Vehículos y consulta por placa
![Vehículos](docs/diagramas/svg/11-vehiculos.svg)

#### CU-014 Parqueaderos
![Parqueaderos](docs/diagramas/svg/12-parqueaderos.svg)

#### CU-005 Cuentas de usuario
![Cuentas](docs/diagramas/svg/13-cuentas.svg)

#### CU-006 Catálogos
![Catálogos](docs/diagramas/svg/14-catalogos.svg)

#### CU-007 Configuración
![Configuración](docs/diagramas/svg/15-configuracion.svg)

### Control de accesos

#### CU-015 Pre-registro de visitantes
![Pre-registro](docs/diagramas/svg/16-pre-registro.svg)

#### CU-016 Ingreso con pre-registro
![Ingreso con pre-registro](docs/diagramas/svg/17-ingreso-con-preregistro.svg)

#### CU-016 y CU-017 Ingreso sin pre-registro
![Ingreso sin pre-registro](docs/diagramas/svg/18-ingreso-sin-preregistro.svg)

#### CU-018, CU-019 y CU-020 Parqueadero de visitante y salida
![Parqueadero de visitante y salida](docs/diagramas/svg/19-parqueadero-visitante-y-salida.svg)

#### CU-021 Historial de accesos
![Historial de accesos](docs/diagramas/svg/20-historial-accesos.svg)

### Operaciones del conjunto

#### CU-022 y CU-023 Correspondencia
![Correspondencia](docs/diagramas/svg/21-correspondencia.svg)

#### CU-029, CU-030 y CU-031 PQRS
![PQRS](docs/diagramas/svg/22-pqrs.svg)

#### CU-024 Zonas comunes
![Zonas comunes](docs/diagramas/svg/25-zonas-comunes.svg)

#### CU-025 y CU-026 Reservar una zona común
![Reservar zona común](docs/diagramas/svg/23-reservar-zona-comun.svg)

#### CU-027 Gestionar reservas
![Gestionar reservas](docs/diagramas/svg/24-reservas-gestion.svg)

#### CU-028 Mantenimientos
![Mantenimientos](docs/diagramas/svg/26-mantenimientos.svg)

### Mecanismos compartidos

#### Borrado físico solo sin historia
![Borrado](docs/diagramas/svg/27-borrado-sin-historia.svg)

#### Máquina de estados por datos
![Máquina de estados](docs/diagramas/svg/28-maquina-de-estados.svg)

## Variables de entorno

| Variable | Obligatoria | Uso |
|---|---|---|
| `JWT_SECRET` | **Sí** (≥ 32 caracteres) | Clave HS256 con que se firman los tokens |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | En producción | Base PostgreSQL (URL en formato JDBC) |
| `BOOTSTRAP_ADMIN_CORREO`, `BOOTSTRAP_ADMIN_CONTRASENA` | No | Crea el primer administrador si no existe ninguno |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN` | No | Correo por Gmail API; si hay `GMAIL_REFRESH_TOKEN`, se usa en lugar de SMTP |
| `MAIL_REMITENTE` | No | Dirección "De:"; con Gmail API, la cuenta que autorizó el token |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | No | Correo por SMTP |
| `FRONTEND_URL_RESTABLECER` | No | Página del front a la que apunta el enlace de restablecer contraseña (por defecto `http://localhost:3000/restablecer-contrasena`) |

Sin `MAIL_HOST` ni `GMAIL_REFRESH_TOKEN` la aplicación arranca igual y solo avisa en el log que no enviará correos.

## Correo con la Gmail API

La Gmail API existe como alternativa porque algunos hosts gratuitos (Render, por ejemplo) bloquean el SMTP saliente; esta va por HTTPS (puerto 443).

1. En Google Cloud, habilita la **Gmail API** y crea un cliente OAuth: de ahí salen `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET`.
2. Con la cuenta que enviará los correos, autoriza una sola vez el scope `https://www.googleapis.com/auth/gmail.send` y guarda el refresh token en `GMAIL_REFRESH_TOKEN`.
3. Pon esa misma cuenta en `MAIL_REMITENTE`.
4. Pasa la pantalla de consentimiento a **In production**: en *Testing* el refresh token caduca a los 7 días.

Los correos salen de forma asíncrona y después del commit: si el caso de uso se revierte, no se envía nada. Nunca se registran tokens ni el cuerpo de los mensajes.

## Despliegue

Con el `Dockerfile` de la raíz se despliega en cualquier host con Docker (Render, por ejemplo). Flyway deja el esquema listo en una base vacía. Para PostgreSQL sirve un servicio gratuito como Neon (usa la URL JDBC y el host directo, no el *pooler*).

El workflow de GitHub Actions (`.github/workflows/ci.yml`) corre las pruebas y verifica que la imagen Docker compile en cada push a `main` y en cada pull request; no publica la imagen.

## Cómo regenerar los diagramas

Las fuentes de los diagramas de secuencia están en [`docs/diagramas/fuente`](docs/diagramas/fuente), una por diagrama, en el formato JSON de [Archify](https://github.com/tt-a1i/archify). Cada una genera un HTML interactivo:

```bash
node <ruta-de-archify>/bin/archify.mjs finalize sequence docs/diagramas/fuente/01-login.json docs/diagramas/html/01-login.html --quality showcase
```

Los HTML y sus recibos van a `docs/diagramas/html` y no se versionan (pesan unos 800 KB cada uno); los SVG de [`docs/diagramas/svg`](docs/diagramas/svg) sí. Cada SVG sale del visor con **Exportar → SVG (claro)** y después se le quitan los fondos de las etiquetas, que tapaban parte de las flechas:

```bash
python docs/diagramas/fuente/quitar-fondos-etiquetas.py
```
