package unisimon.apipropiedadhorizontal.security;

/**
 * UNICA excepcion de autorizacion por URL: el login no puede exigir un token que aun no existe.
 * Todo lo demas se protege con las anotaciones de {@code security.anotaciones}.
 * Agregar aqui una ruta nueva es una decision de seguridad que debe justificarse en CONTEXTO.md.
 */
public final class RutasPublicas {

    public static final String LOGIN = "/api/auth/login";

    private RutasPublicas() {
    }
}
