# API de Propiedad Horizontal

API REST para administrar un conjunto residencial de la Universidad Simón Bolívar (`unisimon`): personas, inmuebles, vehículos y parqueaderos; control de acceso de visitantes; correspondencia, zonas comunes, reservas, mantenimientos y PQRS; y las funciones transversales (sesión, cuentas, catálogos, notificaciones y parámetros).

- **Lenguaje y build:** Java 25, Maven (`./mvnw`), Spring Boot 4.1
- **Persistencia:** Spring Data JPA + PostgreSQL, esquema con **Flyway** (`src/main/resources/db/migration`)
- **Seguridad:** Spring Security + JWT HS256 **stateless**, autorización por etiqueta (`@SoloAdministrador`, `@SoloResidente`...)
- **Correo:** puerto `EnviadorCorreo` con SMTP o Gmail API (OAuth2)
- **Pruebas:** JUnit 5 + MockMvc con H2 en memoria

> Las reglas de arquitectura y seguridad están en [`CONTEXTO.md`](CONTEXTO.md). El contrato de endpoints vive en el repo del front (`ENDPOINTS.md`).

## Contenido

1. [Cómo ejecutarla](#cómo-ejecutarla)
2. [Arquitectura](#arquitectura)
3. [Diagramas de secuencia](#diagramas-de-secuencia)
4. [Casos de uso y diagramas](#casos-de-uso-y-diagramas)
5. [Variables de entorno](#variables-de-entorno)
6. [Despliegue](#despliegue)
7. [Cómo regenerar los diagramas](#cómo-regenerar-los-diagramas)

## Cómo ejecutarla

```bash
# Pruebas (no necesitan Docker ni variables: usan H2 y un secreto de prueba)
./mvnw test

# Desarrollo: completa .env (plantilla en .env.example) y arranca; compose.yaml levanta PostgreSQL
./mvnw spring-boot:run
```

Flujo manual: `POST /api/auth/login {correo, contrasena}` devuelve un `accessToken`; luego se envía `Authorization: Bearer <accessToken>`. `GET /api/auth/me` muestra la identidad del token.

## Arquitectura

```
web/controller   endpoints REST delgados: validan, delegan, devuelven DTO
service          interfaces de casos de uso  →  service/impl  implementaciones
repository       interfaces Spring Data (una por entidad)
domain/model     entidades JPA con su comportamiento propio
security         JWT, SecurityConfig, etiquetas de autorización, UsuarioActual
config           siembras al arrancar (estados, catálogos, administrador inicial)
```

El flujo es siempre `Controller → Service (interfaz) → Repository → Entidad`. Los controladores nunca usan repositorios y las entidades no conocen DTOs ni HTTP. Cada método de controlador lleva una **etiqueta de autorización**; `SeguridadPorEtiquetaTest` falla si falta alguna.

Códigos de error: 400 validación, 401 sin token o credenciales, 403 sin permiso, 404 no existe, 409 duplicado o con historia, 422 regla de negocio.

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

## Casos de uso y diagramas

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

## Variables de entorno

| Variable | Obligatoria | Uso |
|---|---|---|
| `JWT_SECRET` | **Sí** (≥ 32 caracteres) | Clave HS256 |
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | En producción | Base PostgreSQL (URL en formato JDBC) |
| `BOOTSTRAP_ADMIN_CORREO`, `BOOTSTRAP_ADMIN_CONTRASENA` | No | Crea el primer administrador si no existe ninguno |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN` | No | Correo por Gmail API (necesario donde el SMTP está bloqueado) |
| `MAIL_REMITENTE` | No | Dirección "De:"; con Gmail API, la cuenta que autorizó el token |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | No | Correo por SMTP (se usa si no hay `GMAIL_REFRESH_TOKEN`) |
| `FRONTEND_URL_RESTABLECER` | No | Página del front a la que apunta el enlace del correo |

Los secretos van en `.env` (ignorado por git) o en las variables del servicio; nunca en el repositorio.

## Despliegue

Con el `Dockerfile` de la raíz se despliega en cualquier host con Docker (Render, por ejemplo). Flyway deja el esquema listo en una base vacía. Para la base PostgreSQL funciona un servicio gratuito como Neon (usa la URL JDBC y el host directo, no el pooler). Los pasos de la Gmail API están en `CONTEXTO.md`.

## Cómo regenerar los diagramas

Las fuentes están en [`docs/diagramas/fuente`](docs/diagramas/fuente) (una por diagrama, formato JSON de [Archify](https://github.com/tt-a1i/archify)). Cada una genera un HTML interactivo con:

```bash
node <ruta-de-archify>/bin/archify.mjs finalize sequence docs/diagramas/fuente/01-login.json docs/diagramas/01-login.html --quality showcase
```

Los HTML y sus recibos no se versionan (pesan unos 800 KB cada uno); los SVG de `docs/diagramas/svg` sí. Cada SVG sale del HTML con la opción **Exportar → SVG (claro)** del visor.
