package unisimon.apipropiedadhorizontal.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;
import unisimon.apipropiedadhorizontal.util.Correos;

/** Carga credenciales por correo solo para el proceso de login (no se usa para validar peticiones con JWT). */
@Service
public class CuentaUsuarioDetailsService implements UserDetailsService {

    private final CuentaUsuarioRepository cuentas;

    public CuentaUsuarioDetailsService(CuentaUsuarioRepository cuentas) {
        this.cuentas = cuentas;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) {
        return cuentas.findByCorreoAcceso(Correos.normalizar(correo))
                .map(c -> User.withUsername(c.getCorreoAcceso())
                        .password(c.getHashContrasena())
                        .authorities(Roles.autoridad(c.getRol()))
                        .disabled(!c.isActiva())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Cuenta no encontrada"));
    }
}
