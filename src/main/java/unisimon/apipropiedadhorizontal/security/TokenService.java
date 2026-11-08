package unisimon.apipropiedadhorizontal.security;

import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;

/** Puerto para emitir tokens de acceso; permite cambiar el formato sin tocar a los casos de uso (DIP/OCP). */
public interface TokenService {

    TokenEmitido emitir(CuentaUsuario cuenta);
}
