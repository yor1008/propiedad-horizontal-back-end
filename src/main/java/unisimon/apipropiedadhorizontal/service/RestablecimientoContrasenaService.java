package unisimon.apipropiedadhorizontal.service;

/** CU-002 Gestionar contrasena: restablecerla por correo cuando se olvido (flujo publico, sin sesion). */
public interface RestablecimientoContrasenaService {

    /**
     * Envia el enlace al correo de la cuenta. Nunca revela si la cuenta existe: correo desconocido o cuenta inactiva
     * terminan igual, sin error y sin correo.
     */
    void solicitar(String correo);

    /** @throws unisimon.apipropiedadhorizontal.exception.ReglaNegocioException si el token no es valido, expiro o ya se uso */
    void restablecer(String token, String nueva);
}
