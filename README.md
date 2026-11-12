# API de Propiedad Horizontal

API REST para administrar un conjunto residencial: personas, inmuebles, vehículos, parqueaderos, visitantes, correspondencia, zonas comunes, reservas, mantenimientos y PQRS. Proyecto de la Universidad Simón Bolívar.

**Stack:** Java 25 · Spring Boot 4.1 · PostgreSQL + Flyway · Spring Security con JWT · JUnit 5 + H2 · Docker

El contrato de endpoints está en el repo del front (`ENDPOINTS.md`).

## Inicio rápido

```bash
./mvnw test              # pruebas (no necesitan Docker ni variables)
./mvnw spring-boot:run   # desarrollo: copia .env.example a .env; compose.yaml levanta PostgreSQL
```

Entra con `POST /api/auth/login` (`{ "correo", "contrasena" }`) y usa el `accessToken` como `Authorization: Bearer <token>`. El primer administrador se crea con `BOOTSTRAP_ADMIN_CORREO` y `BOOTSTRAP_ADMIN_CONTRASENA`.

## Arquitectura

Flujo: `Controller → Service (interfaz) → Repository → Entidad`, sin saltarse capas.

### Diagrama de componentes

![Diagrama de componentes](docs/diagramas/svg/29-componentes.svg)

### Diagrama de paquetes

![Diagrama de paquetes](docs/diagramas/svg/30-diagrama-paquetes.svg)

Errores: **400** validación · **401** sin token · **403** sin permiso · **404** no existe · **409** duplicado o con historia · **422** regla de negocio.

## Seguridad

- **Sin sesión:** cada petición se valida solo con el JWT.
- **Autorización por etiqueta** (`@SoloAdministrador`, `@SoloVigilante`…) en cada endpoint; una prueba falla si falta alguna.
- **Secretos** solo en `.env` o en las variables del servidor, nunca en el repo.

## Casos de uso

| Módulo | Casos de uso |
|---|---|
| Transversal | CU-001 sesión · 002 contraseña · 003 datos propios · 004 notificaciones · 005 cuentas · 006 catálogos · 007 configuración |
| Administración | CU-008 personas · 009 inmuebles · 010 vínculos · 011 mis inmuebles · 012 vehículos · 013 consulta por placa · 014 parqueaderos |
| Seguridad | CU-015 pre-registro · 016–017 ingreso de visitantes · 018 parqueadero de visitante · 019 salida · 020 visitantes dentro · 021 historial |
| Operaciones | CU-022–023 correspondencia · 024 zonas comunes · 025–027 reservas · 028 mantenimientos · 029–031 PQRS |

Todas las entidades se pueden borrar solo si no tienen historia; si la tienen, se desactivan, cancelan o anulan.

### Diagramas de secuencia

<details>
<summary><b>Arranque y seguridad</b> (5)</summary>

**Arranque de la aplicación**

![Arranque de la aplicación](docs/diagramas/svg/00-arranque.svg)

**CU-001 Iniciar sesión**

![CU-001 Iniciar sesión](docs/diagramas/svg/01-login.svg)

**Petición autenticada y autorización por etiqueta**

![Petición autenticada y autorización por etiqueta](docs/diagramas/svg/02-peticion-autenticada.svg)

**CU-002 Cambiar contraseña**

![CU-002 Cambiar contraseña](docs/diagramas/svg/03-cambiar-contrasena.svg)

**CU-002 Restablecer contraseña por correo**

![CU-002 Restablecer contraseña por correo](docs/diagramas/svg/04-restablecer-contrasena.svg)

</details>

<details>
<summary><b>Notificaciones y perfil</b> (3)</summary>

**CU-004 Generar notificaciones y correo por evento**

![CU-004 Generar notificaciones y correo por evento](docs/diagramas/svg/05-notificaciones-evento.svg)

**CU-004 Consultar notificaciones propias**

![CU-004 Consultar notificaciones propias](docs/diagramas/svg/06-notificaciones-consulta.svg)

**CU-003 y CU-011 Datos propios y mis inmuebles**

![CU-003 y CU-011 Datos propios y mis inmuebles](docs/diagramas/svg/07-perfil-y-mis-inmuebles.svg)

</details>

<details>
<summary><b>Administración</b> (8)</summary>

**CU-008 Alta de persona con verificación de correo**

![CU-008 Alta de persona con verificación de correo](docs/diagramas/svg/08-alta-persona-verificacion.svg)

**CU-008 Editar, desactivar y borrar personas**

![CU-008 Editar, desactivar y borrar personas](docs/diagramas/svg/09-personas-gestion.svg)

**CU-009 y CU-010 Inmuebles y vínculos**

![CU-009 y CU-010 Inmuebles y vínculos](docs/diagramas/svg/10-inmuebles-y-vinculos.svg)

**CU-012 y CU-013 Vehículos y consulta por placa**

![CU-012 y CU-013 Vehículos y consulta por placa](docs/diagramas/svg/11-vehiculos.svg)

**CU-014 Parqueaderos**

![CU-014 Parqueaderos](docs/diagramas/svg/12-parqueaderos.svg)

**CU-005 Cuentas de usuario**

![CU-005 Cuentas de usuario](docs/diagramas/svg/13-cuentas.svg)

**CU-006 Catálogos**

![CU-006 Catálogos](docs/diagramas/svg/14-catalogos.svg)

**CU-007 Configuración**

![CU-007 Configuración](docs/diagramas/svg/15-configuracion.svg)

</details>

<details>
<summary><b>Control de accesos</b> (5)</summary>

**CU-015 Pre-registro de visitantes**

![CU-015 Pre-registro de visitantes](docs/diagramas/svg/16-pre-registro.svg)

**CU-016 Ingreso con pre-registro**

![CU-016 Ingreso con pre-registro](docs/diagramas/svg/17-ingreso-con-preregistro.svg)

**CU-016 y CU-017 Ingreso sin pre-registro**

![CU-016 y CU-017 Ingreso sin pre-registro](docs/diagramas/svg/18-ingreso-sin-preregistro.svg)

**CU-018, CU-019 y CU-020 Parqueadero de visitante y salida**

![CU-018, CU-019 y CU-020 Parqueadero de visitante y salida](docs/diagramas/svg/19-parqueadero-visitante-y-salida.svg)

**CU-021 Historial de accesos**

![CU-021 Historial de accesos](docs/diagramas/svg/20-historial-accesos.svg)

</details>

<details>
<summary><b>Operaciones del conjunto</b> (6)</summary>

**CU-022 y CU-023 Correspondencia**

![CU-022 y CU-023 Correspondencia](docs/diagramas/svg/21-correspondencia.svg)

**CU-029, CU-030 y CU-031 PQRS**

![CU-029, CU-030 y CU-031 PQRS](docs/diagramas/svg/22-pqrs.svg)

**CU-024 Zonas comunes**

![CU-024 Zonas comunes](docs/diagramas/svg/25-zonas-comunes.svg)

**CU-025 y CU-026 Reservar una zona común**

![CU-025 y CU-026 Reservar una zona común](docs/diagramas/svg/23-reservar-zona-comun.svg)

**CU-027 Gestionar reservas**

![CU-027 Gestionar reservas](docs/diagramas/svg/24-reservas-gestion.svg)

**CU-028 Mantenimientos**

![CU-028 Mantenimientos](docs/diagramas/svg/26-mantenimientos.svg)

</details>

<details>
<summary><b>Mecanismos compartidos</b> (2)</summary>

**Borrado físico solo sin historia**

![Borrado físico solo sin historia](docs/diagramas/svg/27-borrado-sin-historia.svg)

**Máquina de estados por datos**

![Máquina de estados por datos](docs/diagramas/svg/28-maquina-de-estados.svg)

</details>

## Variables de entorno

| Variable | Uso |
|---|---|
| `JWT_SECRET` | **Obligatoria** (≥ 32 caracteres): firma de los tokens |
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | Base PostgreSQL en producción |
| `BOOTSTRAP_ADMIN_CORREO`, `BOOTSTRAP_ADMIN_CONTRASENA` | Primer administrador |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN` | Correo por Gmail API (si hay refresh token) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_REMITENTE` | Correo por SMTP / remitente |
| `FRONTEND_URL_RESTABLECER` | Página del front para restablecer contraseña |

## Despliegue

`Dockerfile` listo para cualquier host con Docker (por ejemplo Render); Flyway crea el esquema en una base vacía. GitHub Actions corre las pruebas y compila la imagen en cada push.

<details>
<summary>Configurar el correo con la Gmail API</summary>

1. En Google Cloud, habilita la Gmail API y crea un cliente OAuth (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`).
2. Con la cuenta emisora, autoriza el scope `https://www.googleapis.com/auth/gmail.send` y guarda el refresh token en `GMAIL_REFRESH_TOKEN`.
3. Pon esa cuenta en `MAIL_REMITENTE` y pasa la pantalla de consentimiento a **In production** (en *Testing* el token caduca a los 7 días).

</details>

<details>
<summary>Editar los diagramas</summary>

- Componentes y paquetes: abre los `.drawio.xml` de [`docs/diagramas/fuente`](docs/diagramas/fuente) en [draw.io](https://app.diagrams.net) y exporta el SVG a `docs/diagramas/svg`.
- Secuencia: los `.json` de la misma carpeta se generan con [Archify](https://github.com/tt-a1i/archify) (`archify finalize sequence <json> docs/diagramas/html/<nombre>.html`), se exportan como **SVG (claro)** y se limpian con `python docs/diagramas/fuente/quitar-fondos-etiquetas.py`.

</details>
