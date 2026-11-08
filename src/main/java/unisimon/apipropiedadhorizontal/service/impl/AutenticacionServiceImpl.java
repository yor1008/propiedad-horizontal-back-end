package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;
import unisimon.apipropiedadhorizontal.security.TokenEmitido;
import unisimon.apipropiedadhorizontal.security.TokenService;
import unisimon.apipropiedadhorizontal.service.AutenticacionService;
import unisimon.apipropiedadhorizontal.util.Correos;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AutenticacionServiceImpl implements AutenticacionService {

    private final AuthenticationManager autenticador;
    private final CuentaUsuarioRepository cuentas;
    private final TokenService tokens;
    private final Clock reloj;

    public AutenticacionServiceImpl(AuthenticationManager autenticador, CuentaUsuarioRepository cuentas,
                                    TokenService tokens, Clock reloj) {
        this.autenticador = autenticador;
        this.cuentas = cuentas;
        this.tokens = tokens;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public TokenEmitido iniciarSesion(String correo, String contrasena) {
        String correoNormalizado = Correos.normalizar(correo);
        autenticador.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(correoNormalizado, contrasena));

        CuentaUsuario cuenta = cuentas.findByCorreoAcceso(correoNormalizado).orElseThrow();
        cuenta.registrarAcceso(LocalDateTime.now(reloj));
        return tokens.emitir(cuenta);
    }
}
