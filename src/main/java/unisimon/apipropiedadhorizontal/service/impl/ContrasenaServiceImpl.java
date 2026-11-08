package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;
import unisimon.apipropiedadhorizontal.security.UsuarioActual;
import unisimon.apipropiedadhorizontal.service.ContrasenaService;

@Service
public class ContrasenaServiceImpl implements ContrasenaService {

    private final CuentaUsuarioRepository cuentas;
    private final PasswordEncoder codificador;
    private final UsuarioActual usuarioActual;

    public ContrasenaServiceImpl(CuentaUsuarioRepository cuentas, PasswordEncoder codificador,
                                 UsuarioActual usuarioActual) {
        this.cuentas = cuentas;
        this.codificador = codificador;
        this.usuarioActual = usuarioActual;
    }

    /**
     * Los tokens ya emitidos siguen siendo validos hasta que expiren: la app es stateless y no mantiene
     * lista de revocacion. Por eso la expiracion del JWT debe ser corta.
     */
    @Override
    @Transactional
    public void cambiar(String actual, String nueva) {
        Long id = usuarioActual.cuentaId();
        CuentaUsuario cuenta = cuentas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta", id));

        if (!codificador.matches(actual, cuenta.getHashContrasena())) {
            throw new ReglaNegocioException("La contrasena actual no es correcta");
        }
        if (codificador.matches(nueva, cuenta.getHashContrasena())) {
            throw new ReglaNegocioException("La nueva contrasena debe ser distinta de la actual");
        }
        cuenta.actualizarHashContrasena(codificador.encode(nueva));
    }
}
