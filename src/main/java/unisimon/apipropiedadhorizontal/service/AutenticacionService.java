package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.security.TokenEmitido;

/** CU-001 Gestionar sesion: iniciar sesion. Cerrar sesion es descartar el token en el cliente (stateless). */
public interface AutenticacionService {

    /** @throws org.springframework.security.core.AuthenticationException si las credenciales son invalidas o la cuenta esta inactiva */
    TokenEmitido iniciarSesion(String correo, String contrasena);
}
