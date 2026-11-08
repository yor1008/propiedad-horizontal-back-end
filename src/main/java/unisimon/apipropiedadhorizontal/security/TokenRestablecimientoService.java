package unisimon.apipropiedadhorizontal.security;

import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;

/**
 * Token de un solo proposito para restablecer la contrasena. Sin estado en el servidor: es un JWT firmado con
 * emisor propio (no sirve como token de acceso) y de un solo uso, porque lleva una huella de la contrasena vigente que
 * deja de coincidir en cuanto se cambia.
 */
public interface TokenRestablecimientoService {

    String emitir(CuentaUsuario cuenta);

    /** @throws unisimon.apipropiedadhorizontal.exception.ReglaNegocioException si el token es invalido, expiro o no es de este tipo */
    Reclamos validar(String token);

    /** Huella de la contrasena actual de la cuenta; el token solo vale mientras siga siendo la misma. */
    String huellaDe(CuentaUsuario cuenta);

    record Reclamos(Long cuentaId, String huella) {
    }
}
