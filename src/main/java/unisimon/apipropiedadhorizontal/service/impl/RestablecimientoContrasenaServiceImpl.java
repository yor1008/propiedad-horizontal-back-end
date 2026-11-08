package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;
import unisimon.apipropiedadhorizontal.security.JwtProperties;
import unisimon.apipropiedadhorizontal.security.TokenRestablecimientoService;
import unisimon.apipropiedadhorizontal.service.EnviadorCorreo;
import unisimon.apipropiedadhorizontal.service.RestablecimientoContrasenaService;
import unisimon.apipropiedadhorizontal.util.Correos;

@Service
public class RestablecimientoContrasenaServiceImpl implements RestablecimientoContrasenaService {

    private static final String MENSAJE_INVALIDO = "El enlace de restablecimiento no es valido o ya expiro";

    private final CuentaUsuarioRepository cuentas;
    private final TokenRestablecimientoService tokens;
    private final EnviadorCorreo correo;
    private final PasswordEncoder codificador;
    private final JwtProperties propiedades;
    private final String urlRestablecer;

    public RestablecimientoContrasenaServiceImpl(CuentaUsuarioRepository cuentas, TokenRestablecimientoService tokens,
                                                 EnviadorCorreo correo, PasswordEncoder codificador,
                                                 JwtProperties propiedades,
                                                 @Value("${app.frontend.url-restablecer}") String urlRestablecer) {
        this.cuentas = cuentas;
        this.tokens = tokens;
        this.correo = correo;
        this.codificador = codificador;
        this.propiedades = propiedades;
        this.urlRestablecer = urlRestablecer;
    }

    @Override
    @Transactional(readOnly = true)
    public void solicitar(String correoIngresado) {
        cuentas.findByCorreoAcceso(Correos.normalizar(correoIngresado))
                .filter(CuentaUsuario::isActiva)
                .ifPresent(this::enviarEnlace);
    }

    @Override
    @Transactional
    public void restablecer(String token, String nueva) {
        var reclamos = tokens.validar(token);
        CuentaUsuario cuenta = cuentas.findById(reclamos.cuentaId())
                .filter(CuentaUsuario::isActiva)
                .filter(c -> tokens.huellaDe(c).equals(reclamos.huella()))
                .orElseThrow(() -> new ReglaNegocioException(MENSAJE_INVALIDO));
        cuenta.actualizarHashContrasena(codificador.encode(nueva));
    }

    private void enviarEnlace(CuentaUsuario cuenta) {
        String enlace = urlRestablecer + (urlRestablecer.contains("?") ? "&" : "?") + "token=" + tokens.emitir(cuenta);
        long minutos = propiedades.expiracionRestablecimiento().toMinutes();
        correo.enviar(cuenta.getCorreoAcceso(), "Restablecer su contraseña",
                "Recibimos una solicitud para restablecer la contraseña de su cuenta.\n\n"
                        + "Abra este enlace (válido " + minutos + " minutos y de un solo uso):\n" + enlace + "\n\n"
                        + "Si usted no lo solicitó, ignore este mensaje: su contraseña no cambiará.");
    }
}
