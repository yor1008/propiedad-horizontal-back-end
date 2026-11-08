package unisimon.apipropiedadhorizontal.config;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;
import unisimon.apipropiedadhorizontal.util.Correos;

/**
 * Crea el primer administrador al arrancar, solo si no existe ninguno y si se definieron
 * {@code BOOTSTRAP_ADMIN_CORREO} y {@code BOOTSTRAP_ADMIN_CONTRASENA}. Sin esto nadie podria iniciar sesion
 * para crear las demas cuentas (CU-005). Desde ese momento cambie la contrasena por {@code /api/auth/cambiar-contrasena}.
 * <p>
 * Id: si la tabla de cuentas esta vacia, el contador de identidad se reinicia en 1 y el administrador queda con id 1
 * (sin esto, borrar y recrear la cuenta dejaba ids 2, 3...). Si ya hay otras cuentas el contador no se toca, para no
 * chocar con ids existentes, y se usa el siguiente.
 */
@Slf4j
@Component
@EnableConfigurationProperties(AdministradorInicial.Propiedades.class)
public class AdministradorInicial implements ApplicationRunner {

    @ConfigurationProperties(prefix = "app.bootstrap.admin")
    public record Propiedades(String correo, String contrasena) {
    }

    private final Propiedades propiedades;
    private final CuentaUsuarioRepository cuentas;
    private final PasswordEncoder codificador;
    private final EntityManager entityManager;

    public AdministradorInicial(Propiedades propiedades, CuentaUsuarioRepository cuentas, PasswordEncoder codificador,
                                EntityManager entityManager) {
        this.propiedades = propiedades;
        this.cuentas = cuentas;
        this.codificador = codificador;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (esVacio(propiedades.correo()) || esVacio(propiedades.contrasena())) {
            return;
        }
        if (cuentas.existsByRol(ERolCuenta.ADMINISTRADOR)) {
            return;
        }
        if (propiedades.contrasena().length() < 8) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_CONTRASENA debe tener al menos 8 caracteres");
        }
        String correo = Correos.normalizar(propiedades.correo());
        if (cuentas.count() == 0) {
            reiniciarIdentidad();
        }
        cuentas.save(CuentaUsuario.builder()
                .correoAcceso(correo)
                .hashContrasena(codificador.encode(propiedades.contrasena()))
                .rol(ERolCuenta.ADMINISTRADOR)
                .build());
        log.info("Administrador inicial creado: {}", correo);
    }

    /** Sintaxis valida tanto en PostgreSQL como en H2. La tabla coincide con @Table de CuentaUsuario. */
    private void reiniciarIdentidad() {
        entityManager.createNativeQuery("ALTER TABLE cuenta_usuario ALTER COLUMN id RESTART WITH 1").executeUpdate();
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
