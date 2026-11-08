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
import unisimon.apipropiedadhorizontal.domain.enums.ECategoriaInmueble;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.domain.model.Inmueble;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.TipoPersona;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.repository.CorrespondenciaRepository;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;
import unisimon.apipropiedadhorizontal.repository.InmuebleRepository;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.repository.PqrsRepository;
import unisimon.apipropiedadhorizontal.repository.TipoPersonaRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.repository.VehiculoRepository;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Parqueaderos, correspondencia y PQRS (CU-014, 022, 023, 029, 030, 031). */
@SpringBootTest
@AutoConfigureMockMvc
class OperacionesIntegracionTest {

    private static final String CLAVE = "Contrasena-segura-1";

    @Autowired MockMvc mvc;
    @Autowired unisimon.apipropiedadhorizontal.repository.NotificacionRepository notificaciones;
    @Autowired CuentaUsuarioRepository cuentas;
    @Autowired CorrespondenciaRepository correspondencias;
    @Autowired PqrsRepository pqrs;
    @Autowired VehiculoRepository vehiculos;
    @Autowired PersonaRepository personas;
    @Autowired InmuebleRepository inmuebles;
    @Autowired TipoPersonaRepository tiposPersona;
    @Autowired ValorCatalogoRepository catalogos;
    @Autowired PasswordEncoder codificador;

    Long inmuebleId;
    Long personaId;
    Long tipoInmueble;
    Long tipoCorrespondencia;
    Long tipoPqrs;
    String admin;
    String vigilante;
    String residente;

    @BeforeEach
    void preparar() throws Exception {
        limpiar();
        ValorCatalogo documento = catalogo(ETipoCatalogo.TIPO_DOCUMENTO, "Cedula");
        ValorCatalogo inmuebleTipo = catalogo(ETipoCatalogo.TIPO_INMUEBLE, "Apartamento");
        tipoInmueble = inmuebleTipo.getId();
        tipoCorrespondencia = catalogo(ETipoCatalogo.TIPO_CORRESPONDENCIA, "Paquete").getId();
        tipoPqrs = catalogo(ETipoCatalogo.TIPO_PQRS, "Queja").getId();
        TipoPersona tipo = tiposPersona.save(TipoPersona.builder().nombre("Residente").esResidente(true).build());
        Inmueble inmueble = inmuebles.save(Inmueble.builder()
                .torreBloque("A").numero("101").piso(1).tipo(inmuebleTipo).build());
        inmuebleId = inmueble.getId();
        Persona persona = personas.save(Persona.builder().tipoPersona(tipo).tipoDocumento(documento)
                .numeroDocumento("1001").nombres("Ana").apellidos("Ruiz").inmueble(inmueble)
                .fechaInicioVinculo(LocalDate.now().minusDays(1)).build());
        personaId = persona.getId();
        cuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, null);
        cuenta("vig@ph.com", ERolCuenta.VIGILANTE, null);
        cuenta("ana@ph.com", ERolCuenta.PERSONA_VINCULADA, persona);
        admin = token("admin@ph.com");
        vigilante = token("vig@ph.com");
        residente = token("ana@ph.com");
    }

    @AfterEach
    void limpiar() {
        notificaciones.deleteAll();
        correspondencias.deleteAll();
        pqrs.deleteAll();
        vehiculos.deleteAll();
        cuentas.deleteAll();
        personas.deleteAll();
        inmuebles.deleteAll(inmuebles.findByCategoria(ECategoriaInmueble.PARQUEADERO));
        inmuebles.deleteAll();
        tiposPersona.deleteAll();
        catalogos.deleteAll();
    }

    @Test
    void correspondenciaSeRegistraSeConsultaYSeEntregaUnaSolaVez() throws Exception {
        Long id = id(enviar("post", "/api/correspondencia", vigilante, "{\"inmuebleId\":" + inmuebleId
                + ",\"personaId\":" + personaId + ",\"tipoId\":" + tipoCorrespondencia
                + ",\"remitente\":\"Amazon\",\"descripcion\":\"Caja\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("Pendiente")));
        // Dirigida al inmueble (sin persona): tambien la ve el residente vinculado.
        enviar("post", "/api/correspondencia", vigilante, "{\"inmuebleId\":" + inmuebleId
                + ",\"tipoId\":" + tipoCorrespondencia + "}").andExpect(status().isCreated());

        mvc.perform(get("/api/correspondencia/mias").header("Authorization", "Bearer " + residente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        // Cada correspondencia registrada genero una notificacion para la residente (la de campana del front).
        mvc.perform(get("/api/notificaciones/mias/conteo").header("Authorization", "Bearer " + residente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.noLeidas").value(2));
        mvc.perform(get("/api/notificaciones/mias").header("Authorization", "Bearer " + residente))
                .andExpect(jsonPath("$[0].evento").value("CORRESPONDENCIA_RECIBIDA"))
                .andExpect(jsonPath("$[0].leida").value(false));
        mvc.perform(get("/api/correspondencia?personaId=" + personaId).header("Authorization", "Bearer " + vigilante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        enviar("patch", "/api/correspondencia/" + id + "/entregar", vigilante,
                "{\"nombreRecibe\":\"Ana Ruiz\",\"documentoRecibe\":\"1001\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Entregada"))
                .andExpect(jsonPath("$.fechaEntrega").isNotEmpty());
        enviar("patch", "/api/correspondencia/" + id + "/entregar", vigilante,
                "{\"nombreRecibe\":\"Ana Ruiz\",\"documentoRecibe\":\"1001\"}")
                .andExpect(status().isUnprocessableContent());

        enviar("post", "/api/correspondencia", admin, "{\"inmuebleId\":" + inmuebleId + ",\"tipoId\":" + tipoCorrespondencia + "}").andExpect(status().isForbidden());
        mvc.perform(get("/api/correspondencia/mias").header("Authorization", "Bearer " + vigilante))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/correspondencia")).andExpect(status().isUnauthorized());
    }

    @Test
    void pqrsSeRadicaSeConsultaYSeAtiendeUnaSolaVez() throws Exception {
        Long id = id(enviar("post", "/api/pqrs", residente, "{\"tipoId\":" + tipoPqrs
                + ",\"asunto\":\"Ruido\",\"descripcion\":\"Fiesta\",\"adjunto\":\"foto.png\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("Radicada"))
                .andExpect(jsonPath("$.adjunto").value("foto.png")).andExpect(jsonPath("$.inmueble").value("A-101")));

        mvc.perform(get("/api/pqrs/mias").header("Authorization", "Bearer " + residente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/pqrs").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        enviar("patch", "/api/pqrs/" + id + "/atender", residente, "{\"respuesta\":\"x\"}")
                .andExpect(status().isForbidden());
        enviar("patch", "/api/pqrs/" + id + "/atender", admin, "{\"respuesta\":\"Atendido\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Respondida"))
                .andExpect(jsonPath("$.respuesta").value("Atendido"));
        mvc.perform(get("/api/notificaciones/mias").header("Authorization", "Bearer " + residente))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].evento").value("RESPUESTA_PQRS"));
        enviar("patch", "/api/pqrs/" + id + "/atender", admin, "{\"respuesta\":\"Otra\"}")
                .andExpect(status().isUnprocessableContent());

        enviar("post", "/api/pqrs", vigilante, "{\"tipoId\":" + tipoPqrs + ",\"asunto\":\"a\",\"descripcion\":\"b\"}").andExpect(status().isForbidden());
        enviar("post", "/api/pqrs", residente, "{\"asunto\":\"\"}").andExpect(status().isBadRequest());
    }

    @Test
    void parqueaderoSeCreaAsignaYLiberaSinMezclarseConInmuebles() throws Exception {
        Long id = id(enviar("post", "/api/parqueaderos", admin,
                "{\"torreBloque\":\"P\",\"numero\":\"1\",\"piso\":0,\"tipoId\":" + tipoInmueble + "}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.disponible").value(true)));

        enviar("patch", "/api/parqueaderos/" + id + "/asignar", admin, "{\"inmuebleId\":" + inmuebleId + "}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.inmuebleAsociado").value("A-101"));
        enviar("patch", "/api/parqueaderos/" + id + "/asignar", admin, "{\"inmuebleId\":" + inmuebleId + "}")
                .andExpect(status().isConflict());
        // Un parqueadero no puede recibir parqueadero ni vincular personas.
        enviar("post", "/api/inmuebles/" + id + "/vinculos", admin,
                "{\"personaId\":" + personaId + ",\"tipoPersonaId\":1}").andExpect(status().isUnprocessableContent());

        mvc.perform(get("/api/inmuebles").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/parqueaderos").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.length()").value(1));

        enviar("patch", "/api/parqueaderos/" + id + "/liberar", admin, "")
                .andExpect(status().isOk()).andExpect(jsonPath("$.disponible").value(true));
        mvc.perform(get("/api/parqueaderos").header("Authorization", "Bearer " + vigilante))
                .andExpect(status().isForbidden());
    }

    // ---------- utilidades ----------

    private ValorCatalogo catalogo(ETipoCatalogo tipo, String nombre) {
        return catalogos.save(ValorCatalogo.builder().tipo(tipo).nombre(nombre).build());
    }

    private void cuenta(String correo, ERolCuenta rol, Persona persona) {
        cuentas.save(CuentaUsuario.builder().correoAcceso(correo).hashContrasena(codificador.encode(CLAVE))
                .rol(rol).persona(persona).build());
    }

    private String token(String correo) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.accessToken");
    }

    private ResultActions enviar(String metodo, String ruta, String token, String json) throws Exception {
        var peticion = metodo.equals("post") ? post(ruta) : patch(ruta);
        return mvc.perform(peticion.header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private Long id(ResultActions accion) throws Exception {
        Number id = JsonPath.read(accion.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
