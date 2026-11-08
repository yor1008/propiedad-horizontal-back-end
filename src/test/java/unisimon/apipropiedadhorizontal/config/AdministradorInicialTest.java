package unisimon.apipropiedadhorizontal.config;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;

/** El administrador inicial se crea una sola vez y, con la tabla vacia, siempre con id 1. */
@SpringBootTest
class AdministradorInicialTest {

    @Autowired CuentaUsuarioRepository cuentas;
    @Autowired PasswordEncoder codificador;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transacciones;

    @BeforeEach
    @AfterEach
    void limpiar() {
        cuentas.deleteAll();
    }

    private void arrancar(String correo, String contrasena) {
        var runner = new AdministradorInicial(new AdministradorInicial.Propiedades(correo, contrasena),
                cuentas, codificador, entityManager);
        // El @Transactional del componente real lo aplica Spring; aqui se instancia a mano.
        new TransactionTemplate(transacciones).executeWithoutResult(s -> runner.run(new DefaultApplicationArguments()));
    }

    @Test
    void conLaTablaVaciaElAdministradorQuedaConId1AunqueLaSecuenciaYaHayaAvanzado() {
        // Avanza el contador: crea y borra cuentas para que el siguiente id natural no sea 1.
        for (int i = 0; i < 3; i++) {
            cuentas.save(CuentaUsuario.builder().correoAcceso("x" + i + "@ph.com").hashContrasena("h").rol(ERolCuenta.VIGILANTE).build());
        }
        cuentas.deleteAll();

        arrancar("  Admin@PH.com ", "clave-segura-1");

        var admin = cuentas.findAll().getFirst();
        assertThat(cuentas.count()).isEqualTo(1);
        assertThat(admin.getId()).isEqualTo(1L);
        assertThat(admin.getRol()).isEqualTo(ERolCuenta.ADMINISTRADOR);
        assertThat(admin.getCorreoAcceso()).isEqualTo("admin@ph.com");
        assertThat(codificador.matches("clave-segura-1", admin.getHashContrasena())).isTrue();
    }

    @Test
    void siYaExisteAdministradorNoSeCreaOtro() {
        arrancar("admin@ph.com", "clave-segura-1");
        arrancar("otro@ph.com", "clave-segura-1");

        assertThat(cuentas.count()).isEqualTo(1);
        assertThat(cuentas.findAll().getFirst().getCorreoAcceso()).isEqualTo("admin@ph.com");
    }

    @Test
    void conOtrasCuentasNoSeReiniciaElContadorNiSePisanIds() {
        Long idVigilante = cuentas.save(CuentaUsuario.builder().correoAcceso("vig@ph.com").hashContrasena("h")
                .rol(ERolCuenta.VIGILANTE).build()).getId();

        arrancar("admin@ph.com", "clave-segura-1");

        assertThat(cuentas.count()).isEqualTo(2);
        assertThat(cuentas.findById(idVigilante)).isPresent();
        assertThat(cuentas.existsByRol(ERolCuenta.ADMINISTRADOR)).isTrue();
    }

    @Test
    void sinVariablesNoCreaNada() {
        arrancar("", "");
        arrancar(null, "clave-segura-1");
        assertThat(cuentas.count()).isZero();
    }
}
