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

### Diagrama de despliegue

![Diagrama de despliegue](docs/diagramas/svg/32-despliegue.svg)

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

<details>
<summary><b>Diagramas de casos de uso</b> (uno por módulo)</summary>

![Casos de uso: módulo Transversal](docs/diagramas/Casos%20de%20uso/CU_Modulo_Transversal.drawio.svg)

![Casos de uso: módulo Administración](docs/diagramas/Casos%20de%20uso/CU_Modulo_Administracion.drawio.svg)

![Casos de uso: módulo Seguridad](docs/diagramas/Casos%20de%20uso/CU_Modulo_Seguridad.drawio.svg)

![Casos de uso: módulo Operaciones](docs/diagramas/Casos%20de%20uso/CU_Modulo_Operaciones.drawio.svg)

</details>

Todas las entidades se pueden borrar solo si no tienen historia; si la tienen, se desactivan, cancelan o anulan.

### Diagrama de clases

**Modelo de dominio** (25 entidades y 10 enums)

![Diagrama de clases](docs/diagramas/svg/31-diagrama-clases.svg)

### Diagramas de secuencia por caso de uso

Uno por cada caso de uso del diagrama de casos de uso. Los participantes son clases del diagrama de clases y los mensajes son sus operaciones (UML 2.5.1, cláusula 17).

<details>
<summary><b>Módulo Transversal</b> (7)</summary>

![CU-001 Gestionar sesión](docs/diagramas/Secuencia/cu-001-gestionar-sesion.svg)

![CU-002 Gestionar contraseña](docs/diagramas/Secuencia/cu-002-gestionar-contrasena.svg)

![CU-003 Gestionar datos propios](docs/diagramas/Secuencia/cu-003-gestionar-datos-propios.svg)

![CU-004 Consultar notificaciones](docs/diagramas/Secuencia/cu-004-consultar-notificaciones.svg)

![CU-005 Gestionar cuentas de usuario](docs/diagramas/Secuencia/cu-005-gestionar-cuentas-de-usuario.svg)

![CU-006 Administrar catálogos](docs/diagramas/Secuencia/cu-006-administrar-catalogos.svg)

![CU-007 Configurar parámetros y notificaciones](docs/diagramas/Secuencia/cu-007-configurar-parametros-y-notificaciones.svg)

</details>

<details>
<summary><b>Módulo Administración</b> (7)</summary>

![CU-008 Gestionar personas](docs/diagramas/Secuencia/cu-008-gestionar-personas.svg)

![CU-009 Gestionar inmuebles](docs/diagramas/Secuencia/cu-009-gestionar-inmuebles.svg)

![CU-010 Gestionar vinculaciones](docs/diagramas/Secuencia/cu-010-gestionar-vinculaciones.svg)

![CU-011 Consultar inmuebles propios](docs/diagramas/Secuencia/cu-011-consultar-inmuebles-propios.svg)

![CU-012 Gestionar vehículos](docs/diagramas/Secuencia/cu-012-gestionar-vehiculos.svg)

![CU-013 Consultar vehículo por placa](docs/diagramas/Secuencia/cu-013-consultar-vehiculo-por-placa.svg)

![CU-014 Gestionar parqueaderos](docs/diagramas/Secuencia/cu-014-gestionar-parqueaderos.svg)

</details>

<details>
<summary><b>Módulo Seguridad</b> (7)</summary>

![CU-015 Gestionar pre-registros de visitantes](docs/diagramas/Secuencia/cu-015-gestionar-pre-registros-de-visitantes.svg)

![CU-016 Autorizar ingreso de visitante](docs/diagramas/Secuencia/cu-016-autorizar-ingreso-de-visitante.svg)

![CU-017 Autorizar ingreso sin pre-registro](docs/diagramas/Secuencia/cu-017-autorizar-ingreso-sin-pre-registro.svg)

![CU-018 Asignar parqueadero de visitante](docs/diagramas/Secuencia/cu-018-asignar-parqueadero-de-visitante.svg)

![CU-019 Registrar salida de visitante](docs/diagramas/Secuencia/cu-019-registrar-salida-de-visitante.svg)

![CU-020 Consultar visitantes dentro del conjunto](docs/diagramas/Secuencia/cu-020-consultar-visitantes-dentro-del-conjunto.svg)

![CU-021 Consultar historial de accesos](docs/diagramas/Secuencia/cu-021-consultar-historial-de-accesos.svg)

</details>

<details>
<summary><b>Módulo Operaciones</b> (10)</summary>

![CU-022 Gestionar correspondencia](docs/diagramas/Secuencia/cu-022-gestionar-correspondencia.svg)

![CU-023 Consultar correspondencia](docs/diagramas/Secuencia/cu-023-consultar-correspondencia.svg)

![CU-024 Gestionar zonas comunes](docs/diagramas/Secuencia/cu-024-gestionar-zonas-comunes.svg)

![CU-025 Reservar zona común](docs/diagramas/Secuencia/cu-025-reservar-zona-comun.svg)

![CU-026 Gestionar reservas propias](docs/diagramas/Secuencia/cu-026-gestionar-reservas-propias.svg)

![CU-027 Gestionar reservas](docs/diagramas/Secuencia/cu-027-gestionar-reservas.svg)

![CU-028 Gestionar mantenimientos](docs/diagramas/Secuencia/cu-028-gestionar-mantenimientos.svg)

![CU-029 Radicar PQRS](docs/diagramas/Secuencia/cu-029-radicar-pqrs.svg)

![CU-030 Consultar PQRS propias](docs/diagramas/Secuencia/cu-030-consultar-pqrs-propias.svg)

![CU-031 Atender PQRS](docs/diagramas/Secuencia/cu-031-atender-pqrs.svg)

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

- Componentes: abre el `.drawio.xml` de [`docs/diagramas/fuente`](docs/diagramas/fuente) en [draw.io](https://app.diagrams.net) y exporta el SVG a `docs/diagramas/svg`.
- Clases: abre [`docs/diagramas/Clases/DIAGRAMA DE CLASE.mejorado.xml`](docs/diagramas/Clases/DIAGRAMA%20DE%20CLASE.mejorado.xml) en draw.io y exporta el SVG a `docs/diagramas/svg/31-diagrama-clases.svg` (fondo claro).
- Secuencia: `python docs/diagramas/fuente/secuencia-cu/gen_secuencia_casos_uso.py` exporta los SVG de [`docs/diagramas/Secuencia`](docs/diagramas/Secuencia) desde `secuencia-casos-de-uso.xml` (editable en draw.io; `--regenerar-xml` lo reescribe y descarta retoques).
- Despliegue: `python docs/diagramas/fuente/gen_despliegue.py` escribe `docs/diagramas/fuente/32-despliegue.drawio.xml`; luego `python docs/diagramas/fuente/secuencia-cu/exportar_drawio_svg.py docs/diagramas/fuente/32-despliegue.drawio.xml docs/diagramas/svg/32-despliegue.svg`.
- Casos de uso: edita los `.drawio` de [`docs/diagramas/Casos de uso`](docs/diagramas/Casos%20de%20uso) y expórtalos con `exportar_drawio_svg.py` al `.drawio.svg` del mismo nombre.

</details>
