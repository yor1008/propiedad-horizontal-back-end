package unisimon.apipropiedadhorizontal.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;
import unisimon.apipropiedadhorizontal.support.CorreoCapturado;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Restablecer la contrasena por correo (CU-002): flujo publico con token de un solo uso y sin estado. */
@SpringBootTest
@AutoConfigureMockMvc
class RestablecerContrasenaIntegracionTest {

    private static final String CLAVE = "Contrasena-segura-1";
    private static final String NUEVA = "NuevaClave-456";
    private static final Pattern TOKEN = Pattern.compile("token=([\\w\\-.]+)");

    @Autowired MockMvc mvc;
    @Autowired CuentaUsuarioRepository cuentas;
    @Autowired PasswordEncoder codificador;
    @Autowired CorreoCapturado correo;

    @BeforeEach
    void preparar() {
        limpiar();
        cuenta("ana@ph.com", true);
        cuenta("baja@ph.com", false);
    }

    @AfterEach
    void limpiar() {
        cuentas.deleteAll();
        correo.limpiar();
    }

    @Test
    void elFlujoCompletoCambiaLaContrasenaYElEnlaceSoloSirveUnaVez() throws Exception {
        olvide("  ANA@PH.COM ").andExpect(status().isNoContent());
        assertThat(correo.enviados()).hasSize(1);
        assertThat(correo.enviados().getFirst().destinatario()).isEqualTo("ana@ph.com");
        assertThat(correo.enviados().getFirst().texto()).contains("http://localhost/restablecer-contrasena?token=");
        String token = tokenDelCorreo();

        restablecer(token, "corta").andExpect(status().isBadRequest());
        restablecer(token, NUEVA).andExpect(status().isNoContent());

        login("ana@ph.com", CLAVE).andExpect(status().isUnauthorized());
        login("ana@ph.com", NUEVA).andExpect(status().isOk());
        // De un solo uso: la huella de la contrasena ya no coincide.
        restablecer(token, "Otra-clave-789").andExpect(status().isUnprocessableContent());
        login("ana@ph.com", NUEVA).andExpect(status().isOk());
    }

    @Test
    void solicitarNuncaRevelaSiLaCuentaExiste() throws Exception {
        olvide("noexiste@ph.com").andExpect(status().isNoContent());
        olvide("baja@ph.com").andExpect(status().isNoContent());
        assertThat(correo.enviados()).isEmpty();
        olvide("").andExpect(status().isBadRequest());
    }

    @Test
    void tokensInvalidosSeRechazan() throws Exception {
        restablecer("esto-no-es-un-token", NUEVA).andExpect(status().isUnprocessableContent());
        // Un token de acceso (login) no sirve para restablecer...
        String acceso = JsonPath.read(login("ana@ph.com", CLAVE).andReturn().getResponse().getContentAsString(),
                "$.accessToken");
        restablecer(acceso, NUEVA).andExpect(status().isUnprocessableContent());
        // ...ni un token de restablecimiento sirve como token de acceso.
        olvide("ana@ph.com").andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenDelCorreo()))
                .andExpect(status().isUnauthorized());
        login("ana@ph.com", CLAVE).andExpect(status().isOk());
    }

    @Test
    void siLaCuentaSeDesactivaElEnlaceDejaDeServir() throws Exception {
        olvide("ana@ph.com").andExpect(status().isNoContent());
        String token = tokenDelCorreo();
        var ana = cuentas.findByCorreoAcceso("ana@ph.com").orElseThrow();
        ana.desactivar();
        cuentas.save(ana);
        restablecer(token, NUEVA).andExpect(status().isUnprocessableContent());
    }

    // ---------- utilidades ----------

    private void cuenta(String correoAcceso, boolean activa) {
        cuentas.save(CuentaUsuario.builder().correoAcceso(correoAcceso).hashContrasena(codificador.encode(CLAVE))
                .rol(ERolCuenta.VIGILANTE).activa(activa).build());
    }

    private String tokenDelCorreo() {
        Matcher m = TOKEN.matcher(correo.enviados().getLast().texto());
        assertThat(m.find()).as("el correo trae el enlace con token").isTrue();
        return m.group(1);
    }

    private ResultActions olvide(String correoIngresado) throws Exception {
        return mvc.perform(post("/api/auth/olvide-contrasena").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correoIngresado + "\"}"));
    }

    private ResultActions restablecer(String token, String nueva) throws Exception {
        return mvc.perform(post("/api/auth/restablecer-contrasena").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"nueva\":\"" + nueva + "\"}"));
    }

    private ResultActions login(String correoAcceso, String contrasena) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correoAcceso + "\",\"contrasena\":\"" + contrasena + "\"}"));
    }
}
