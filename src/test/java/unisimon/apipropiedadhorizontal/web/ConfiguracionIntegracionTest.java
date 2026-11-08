package unisimon.apipropiedadhorizontal.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.repository.ConfiguracionGeneralRepository;
import unisimon.apipropiedadhorizontal.repository.ConfiguracionNotificacionRepository;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Parametros generales y notificaciones por evento (CU-007). */
@SpringBootTest
@AutoConfigureMockMvc
class ConfiguracionIntegracionTest {

    private static final String CLAVE = "Contrasena-segura-1";

    @Autowired MockMvc mvc;
    @Autowired CuentaUsuarioRepository cuentas;
    @Autowired ConfiguracionGeneralRepository general;
    @Autowired ConfiguracionNotificacionRepository notificaciones;
    @Autowired PasswordEncoder codificador;

    String admin;
    String vigilante;

    @BeforeEach
    void preparar() throws Exception {
        limpiar();
        cuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR);
        cuenta("vig@ph.com", ERolCuenta.VIGILANTE);
        admin = token("admin@ph.com");
        vigilante = token("vig@ph.com");
    }

    @AfterEach
    void limpiar() {
        cuentas.deleteAll();
        // Que otras pruebas (y el siguiente arranque) vean los valores por defecto.
        general.deleteAll();
        notificaciones.deleteAll();
    }

    @Test
    void soloElAdministradorConfigura() throws Exception {
        mvc.perform(get("/api/configuracion/general")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/configuracion/notificaciones")).andExpect(status().isUnauthorized());
        for (String token : new String[]{vigilante}) {
            mvc.perform(get("/api/configuracion/general").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
            enviar(put("/api/configuracion/general"), token, "{\"maxVehiculosPorInmueble\":2,\"plazoRespuestaPqrsDias\":5}")
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/configuracion/notificaciones").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
            enviar(put("/api/configuracion/notificaciones/INGRESO_VISITANTE"), token, "{\"activa\":false,\"enviarCorreo\":false}")
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void parametrosGeneralesSeCreanPorDefectoSeActualizanYSeValidan() throws Exception {
        // Sin fila (base nueva): se crea con los valores por defecto.
        mvc.perform(get("/api/configuracion/general").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxVehiculosPorInmueble").value(0))
                .andExpect(jsonPath("$.plazoRespuestaPqrsDias").value(15));
        enviar(put("/api/configuracion/general"), admin, "{\"maxVehiculosPorInmueble\":2,\"plazoRespuestaPqrsDias\":10}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxVehiculosPorInmueble").value(2))
                .andExpect(jsonPath("$.plazoRespuestaPqrsDias").value(10));
        mvc.perform(get("/api/configuracion/general").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.maxVehiculosPorInmueble").value(2));
        // Sigue habiendo una unica fila.
        org.assertj.core.api.Assertions.assertThat(general.count()).isEqualTo(1);
        enviar(put("/api/configuracion/general"), admin, "{\"maxVehiculosPorInmueble\":-1,\"plazoRespuestaPqrsDias\":10}")
                .andExpect(status().isBadRequest());
        enviar(put("/api/configuracion/general"), admin, "{\"maxVehiculosPorInmueble\":1,\"plazoRespuestaPqrsDias\":0}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void notificacionesListanTodosLosEventosYSeConfiguranPorEvento() throws Exception {
        // Sin filas: aparecen los 4 eventos con el valor por defecto (activa, sin correo).
        mvc.perform(get("/api/configuracion/notificaciones").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].evento").value("CORRESPONDENCIA_RECIBIDA"))
                .andExpect(jsonPath("$[0].activa").value(true))
                .andExpect(jsonPath("$[0].enviarCorreo").value(false));
        enviar(put("/api/configuracion/notificaciones/INGRESO_VISITANTE"), admin, "{\"activa\":false,\"enviarCorreo\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evento").value("INGRESO_VISITANTE"))
                .andExpect(jsonPath("$.activa").value(false))
                .andExpect(jsonPath("$.enviarCorreo").value(true));
        // Actualizar de nuevo no duplica la fila (evento es unico).
        enviar(put("/api/configuracion/notificaciones/INGRESO_VISITANTE"), admin, "{\"activa\":true,\"enviarCorreo\":false}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(true));
        enviar(put("/api/configuracion/notificaciones/INGRESO_VISITANTE"), admin, "{\"activa\":false,\"enviarCorreo\":false}")
                .andExpect(status().isOk());
        mvc.perform(get("/api/configuracion/notificaciones").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[1].evento").value("INGRESO_VISITANTE"))
                .andExpect(jsonPath("$[1].activa").value(false));
        // Validaciones.
        enviar(put("/api/configuracion/notificaciones/NO_EXISTE"), admin, "{\"activa\":true,\"enviarCorreo\":false}")
                .andExpect(status().isBadRequest());
        enviar(put("/api/configuracion/notificaciones/INGRESO_VISITANTE"), admin, "{\"activa\":true}")
                .andExpect(status().isBadRequest());
    }

    // ---------- utilidades ----------

    private void cuenta(String correo, ERolCuenta rol) {
        cuentas.save(CuentaUsuario.builder().correoAcceso(correo).hashContrasena(codificador.encode(CLAVE)).rol(rol).build());
    }

    private String token(String correo) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.accessToken");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String token, String json) throws Exception {
        return mvc.perform(peticion.header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
