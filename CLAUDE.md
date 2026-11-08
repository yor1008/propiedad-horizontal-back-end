# Instrucciones del proyecto

**Antes de cualquier acción (crear, editar, borrar, refactorizar, agregar dependencias o endpoints), lee
[`CONTEXTO.md`](CONTEXTO.md) y respeta sus reglas.** Es la fuente de verdad de arquitectura, seguridad
(JWT stateless, autorización por etiqueta y no por URL), SOLID y convenciones.

- Si una petición contradice `CONTEXTO.md`, avisa al usuario antes de proceder.
- Si cambias una regla o completas un caso de uso, actualiza `CONTEXTO.md` en el mismo cambio.
- **Contrato de endpoints:** vive en el repo del front, `C:\Users\manue\Downloads\propiedad-horizontal-front-end\ENDPOINTS.md`
  (aquí **no** hay `ENDPOINTS.md`; no lo recrees). Al agregar o cambiar un endpoint, DTO o código de error, actualiza
  ese archivo en el mismo cambio: es cómo el front se entera de cómo va quedando el back.
- Toda clase, atributo, enum, método o relación nueva (o corrección al UML) que no esté en el diagrama se anota en
  [`CAMBIOS_DIAGRAMA.md`](CAMBIOS_DIAGRAMA.md), incluidas las ideas que propongas (marcadas 💡).
- `diagramas_uml/` está en `.gitignore`: es solo referencia local; nunca la versiones.
- Verificación mínima: `./mvnw test`.

- **PROHIBIDO leer, editar, imprimir o inspeccionar `.env`** (ni con Read, cat, grep, etc.): contiene secretos
  (JWT y, a futuro, la key de Google Cloud/OAuth2). Solo se documentan nombres de variables en `.env.example`.
  Esta regla tambien esta forzada en `.claude/settings.json`.
