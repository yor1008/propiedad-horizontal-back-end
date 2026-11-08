package unisimon.apipropiedadhorizontal.security;

/**
 * UNICAS excepciones de autorizacion por URL: rutas que no pueden exigir un token porque quien las usa aun no tiene
 * sesion (iniciar sesion y recuperar una contrasena olvidada). Todo lo demas se protege con las anotaciones de
 * {@code security.anotaciones}. Agregar aqui una ruta nueva es una decision de seguridad que debe justificarse en
 * CONTEXTO.md.
 */
public final class RutasPublicas {

    public static final String LOGIN = "/api/auth/login";
    public static final String OLVIDE_CONTRASENA = "/api/auth/olvide-contrasena";
    public static final String RESTABLECER_CONTRASENA = "/api/auth/restablecer-contrasena";

    private RutasPublicas() {
    }
}
