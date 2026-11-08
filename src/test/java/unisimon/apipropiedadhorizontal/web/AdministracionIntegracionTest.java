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
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.domain.model.TipoPersona;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;
import unisimon.apipropiedadhorizontal.repository.InmuebleRepository;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.repository.TipoPersonaRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.repository.VehiculoRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Personas, inmuebles, vinculaciones y vehiculos (CU-008, 009, 010, 012, 013). */
@SpringBootTest
@AutoConfigureMockMvc
class AdministracionIntegracionTest {

    private static final String CLAVE = "Contrasena-segura-1";

    @Autowired MockMvc mvc;
    @Autowired unisimon.apipropiedadhorizontal.repository.NotificacionRepository notificaciones;
    @Autowired CuentaUsuarioRepository cuentas;
    @Autowired VehiculoRepository vehiculos;
    @Autowired PersonaRepository personas;
    @Autowired InmuebleRepository inmuebles;
    @Autowired TipoPersonaRepository tiposPersona;
    @Autowired ValorCatalogoRepository catalogos;
    @Autowired PasswordEncoder codificador;

    Long tipoDocumento;
    Long tipoInmueble;
    Long tipoVehiculo;
    Long tipoResidente;
    String admin;
    String vigilante;

    @BeforeEach
    void preparar() throws Exception {
        limpiar();
        tipoDocumento = catalogo(ETipoCatalogo.TIPO_DOCUMENTO, "Cedula");
        tipoInmueble = catalogo(ETipoCatalogo.TIPO_INMUEBLE, "Apartamento");
        tipoVehiculo = catalogo(ETipoCatalogo.TIPO_VEHICULO, "Moto");
        tipoResidente = tiposPersona.save(TipoPersona.builder().nombre("Residente").esResidente(true).build()).getId();
        cuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR);
        cuenta("vig@ph.com", ERolCuenta.VIGILANTE);
        admin = token("admin@ph.com");
        vigilante = token("vig@ph.com");
    }

    @AfterEach
    void limpiar() {
        notificaciones.deleteAll();
        vehiculos.deleteAll();
        cuentas.deleteAll();
        personas.deleteAll();
        inmuebles.deleteAll();
        tiposPersona.deleteAll();
        catalogos.deleteAll();
    }

    @Test
    void flujoCompletoPersonaInmuebleVinculoYVehiculo() throws Exception {
        Long personaId = id(crearPersona("1001"));
        mvc.perform(get("/api/personas").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipoPersona").value("Residente"))
                .andExpect(jsonPath("$[0].vinculoVigente").value(false));

        Long inmuebleId = id(enviarPost("/api/inmuebles", admin,
                "{\"torreBloque\":\"A\",\"numero\":\"101\",\"piso\":1,\"tipoId\":" + tipoInmueble + "}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ocupado").value(false)));

        // Vehiculo sin vinculo vigente: regla de negocio.
        enviarPost("/api/vehiculos", admin, vehiculoJson(personaId, inmuebleId, "abc123")).andExpect(status().isUnprocessableContent());

        enviarPost("/api/inmuebles/" + inmuebleId + "/vinculos", admin,
                "{\"personaId\":" + personaId + ",\"tipoPersonaId\":" + tipoResidente + "}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.vinculoVigente").value(true));
        mvc.perform(get("/api/inmuebles/" + inmuebleId).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.ocupado").value(true));

        enviarPost("/api/vehiculos", admin, vehiculoJson(personaId, inmuebleId, "abc123"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.placa").value("ABC123"));
        enviarPost("/api/vehiculos", admin, vehiculoJson(personaId, inmuebleId, "ABC123")).andExpect(status().isConflict());

        mvc.perform(get("/api/vehiculos/placa/abc123").header("Authorization", "Bearer " + vigilante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.inmueble").value("A-101"));
        mvc.perform(get("/api/vehiculos/placa/ZZZ999").header("Authorization", "Bearer " + vigilante))
                .andExpect(status().isNotFound());

        enviarPatch("/api/personas/" + personaId + "/finalizar-vinculo", admin, "{\"motivo\":\"Se mudo\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.vinculoVigente").value(false));
        mvc.perform(get("/api/inmuebles/" + inmuebleId).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.ocupado").value(false));
    }

    @Test
    void personaDuplicadaYValidaciones() throws Exception {
        crearPersona("2002").andExpect(status().isCreated());
        crearPersona("2002").andExpect(status().isConflict());
        enviarPost("/api/personas", admin, "{\"nombres\":\"\"}").andExpect(status().isBadRequest());
    }

    @Test
    void autorizacionPorEtiqueta() throws Exception {
        mvc.perform(get("/api/personas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/personas").header("Authorization", "Bearer " + vigilante)).andExpect(status().isForbidden());
        mvc.perform(get("/api/inmuebles").header("Authorization", "Bearer " + vigilante)).andExpect(status().isForbidden());
        mvc.perform(get("/api/vehiculos/placa/ABC123").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
    }

    @Test
    void personaSeEditaYSeBorraSoloSiNoTieneHistoria() throws Exception {
        Long libre = id(crearPersona("3003").andExpect(status().isCreated()));
        String cambios = "{\"nombres\":\"Ana Maria\",\"apellidos\":\"Ruiz\",\"telefono\":\"3111111111\",\"correo\":\"NUEVO@ph.com\"}";
        mvc.perform(put("/api/personas/" + libre).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cambios))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombres").value("Ana Maria"))
                .andExpect(jsonPath("$.correo").value("nuevo@ph.com"))
                .andExpect(jsonPath("$.numeroDocumento").value("3003"));
        mvc.perform(put("/api/personas/" + libre).header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombres\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/personas/99999").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(cambios)).andExpect(status().isNotFound());

        mvc.perform(delete("/api/personas/" + libre).header("Authorization", "Bearer " + vigilante)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/personas/" + libre).header("Authorization", "Bearer " + admin)).andExpect(status().isNoContent());
        mvc.perform(get("/api/personas/" + libre).header("Authorization", "Bearer " + admin)).andExpect(status().isNotFound());

        // Con vinculo: se puede desactivar y reactivar (la ocupacion del inmueble sigue), pero no borrar.
        Long personaId = id(crearPersona("4004"));
        Long inmuebleId = id(enviarPost("/api/inmuebles", admin,
                "{\"torreBloque\":\"B\",\"numero\":\"202\",\"piso\":2,\"tipoId\":" + tipoInmueble + "}"));
        enviarPost("/api/inmuebles/" + inmuebleId + "/vinculos", admin,
                "{\"personaId\":" + personaId + ",\"tipoPersonaId\":" + tipoResidente + "}").andExpect(status().isCreated());
        enviarPatch("/api/personas/" + personaId + "/desactivar", admin, "{\"motivo\":\"Fallecio\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(false))
                .andExpect(jsonPath("$.vinculoVigente").value(false));
        mvc.perform(get("/api/inmuebles/" + inmuebleId).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.ocupado").value(false));
        mvc.perform(patch("/api/personas/" + personaId + "/reactivar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(true));
        mvc.perform(get("/api/inmuebles/" + inmuebleId).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.ocupado").value(true));
        mvc.perform(delete("/api/personas/" + personaId).header("Authorization", "Bearer " + admin)).andExpect(status().isConflict());
    }

    @Test
    void inmuebleSeEditaSeDesactivaYSoloSeBorraSinHistoria() throws Exception {
        Long casa = catalogo(ETipoCatalogo.TIPO_INMUEBLE, "Casa");
        Long a = id(enviarPost("/api/inmuebles", admin, inmueble("A", "101", 1, tipoInmueble)).andExpect(status().isCreated()));
        Long b = id(enviarPost("/api/inmuebles", admin, inmueble("A", "102", 1, tipoInmueble)).andExpect(status().isCreated()));

        // Editar: cambia datos y tipo; no puede chocar con otro inmueble, pero si repetir los propios.
        mvc.perform(put("/api/inmuebles/" + a).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(inmueble("A", "103", 3, casa)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numero").value("103"))
                .andExpect(jsonPath("$.piso").value(3)).andExpect(jsonPath("$.tipo").value("Casa"));
        mvc.perform(put("/api/inmuebles/" + a).header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(inmueble("A", "102", 3, casa))).andExpect(status().isConflict());
        mvc.perform(put("/api/inmuebles/" + a).header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(inmueble("A", "103", 3, casa))).andExpect(status().isOk());
        mvc.perform(put("/api/inmuebles/" + a).header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(inmueble("A", "103", 3, 999999L))).andExpect(status().isNotFound());
        mvc.perform(put("/api/inmuebles/" + a).header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"numero\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/inmuebles/999999").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(inmueble("A", "103", 3, casa))).andExpect(status().isNotFound());
        mvc.perform(put("/api/inmuebles/" + a).header("Authorization", "Bearer " + vigilante)
                .contentType(MediaType.APPLICATION_JSON).content(inmueble("A", "103", 3, casa))).andExpect(status().isForbidden());

        // Un parqueadero no se gestiona por aqui.
        Long parqueadero = id(enviarPost("/api/parqueaderos", admin,
                "{\"torreBloque\":\"P\",\"numero\":\"1\",\"piso\":0,\"tipoId\":" + tipoInmueble + "}").andExpect(status().isCreated()));
        mvc.perform(put("/api/inmuebles/" + parqueadero).header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(inmueble("P", "1", 0, tipoInmueble))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/inmuebles/" + parqueadero).header("Authorization", "Bearer " + admin)).andExpect(status().isNotFound());
        enviarPatch("/api/inmuebles/" + parqueadero + "/desactivar", admin, "{\"motivo\":\"x\"}").andExpect(status().isNotFound());

        // Con una persona vinculada vigente no se puede desactivar; sin vinculos si, y vuelve a poder activarse.
        Long persona = id(crearPersona("7007").andExpect(status().isCreated()));
        enviarPost("/api/inmuebles/" + a + "/vinculos", admin, "{\"personaId\":" + persona + ",\"tipoPersonaId\":" + tipoResidente + "}")
                .andExpect(status().isCreated());
        enviarPatch("/api/inmuebles/" + a + "/desactivar", admin, "{\"motivo\":\"Remodelacion\"}").andExpect(status().isUnprocessableContent());
        enviarPatch("/api/personas/" + persona + "/finalizar-vinculo", admin, "{\"motivo\":\"Se mudo\"}").andExpect(status().isOk());
        enviarPatch("/api/inmuebles/" + a + "/desactivar", admin, "{}").andExpect(status().isBadRequest());
        enviarPatch("/api/inmuebles/" + a + "/desactivar", vigilante, "{\"motivo\":\"x\"}").andExpect(status().isForbidden());
        enviarPatch("/api/inmuebles/" + a + "/desactivar", admin, "{\"motivo\":\"Remodelacion\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        enviarPost("/api/inmuebles/" + a + "/vinculos", admin, "{\"personaId\":" + persona + ",\"tipoPersonaId\":" + tipoResidente + "}")
                .andExpect(status().isUnprocessableContent());
        mvc.perform(patch("/api/inmuebles/" + a + "/reactivar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));

        // Borrado fisico: con historia (tuvo una persona vinculada) no; el que nunca se uso, si.
        mvc.perform(delete("/api/inmuebles/" + a).header("Authorization", "Bearer " + admin)).andExpect(status().isConflict());
        mvc.perform(delete("/api/inmuebles/" + b).header("Authorization", "Bearer " + vigilante)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/inmuebles/" + b).header("Authorization", "Bearer " + admin)).andExpect(status().isNoContent());
        mvc.perform(get("/api/inmuebles/" + b).header("Authorization", "Bearer " + admin)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/inmuebles/" + b).header("Authorization", "Bearer " + admin)).andExpect(status().isNotFound());
    }

    @Test
    void inmuebleConParqueaderoAsignadoNoSeBorra() throws Exception {
        Long a = id(enviarPost("/api/inmuebles", admin, inmueble("B", "201", 2, tipoInmueble)).andExpect(status().isCreated()));
        Long parqueadero = id(enviarPost("/api/parqueaderos", admin,
                "{\"torreBloque\":\"P\",\"numero\":\"9\",\"piso\":0,\"tipoId\":" + tipoInmueble + "}").andExpect(status().isCreated()));
        enviarPatch("/api/parqueaderos/" + parqueadero + "/asignar", admin, "{\"inmuebleId\":" + a + "}").andExpect(status().isOk());
        mvc.perform(delete("/api/inmuebles/" + a).header("Authorization", "Bearer " + admin)).andExpect(status().isConflict());
        enviarPatch("/api/parqueaderos/" + parqueadero + "/liberar", admin, "").andExpect(status().isOk());
        mvc.perform(delete("/api/inmuebles/" + a).header("Authorization", "Bearer " + admin)).andExpect(status().isNoContent());
    }

    @Test
    void tiposDePersonaSeListanParaCualquierAutenticadoYSoloActivos() throws Exception {
        tiposPersona.save(TipoPersona.builder().nombre("Inactivo").activo(false).build());
        for (String token : new String[]{admin, vigilante}) {
            mvc.perform(get("/api/tipos-persona").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(tipoResidente))
                    .andExpect(jsonPath("$[0].nombre").value("Residente"))
                    .andExpect(jsonPath("$[0].esResidente").value(true));
        }
        mvc.perform(get("/api/tipos-persona")).andExpect(status().isUnauthorized());
        // El id devuelto es el que acepta POST /api/personas.
        crearPersona("6006").andExpect(status().isCreated());
    }

    @Test
    void perfilPropioSoloParaPersonaVinculadaYSiempreSobreSuPersona() throws Exception {
        Long personaId = id(crearPersona("5005"));
        var persona = personas.findById(personaId).orElseThrow();
        cuentas.save(CuentaUsuario.builder().correoAcceso("ana@ph.com").hashContrasena(codificador.encode(CLAVE))
                .rol(ERolCuenta.PERSONA_VINCULADA).persona(persona).build());
        String ana = token("ana@ph.com");

        mvc.perform(get("/api/perfil").header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(personaId))
                .andExpect(jsonPath("$.nombres").value("Ana"));
        mvc.perform(put("/api/perfil").header("Authorization", "Bearer " + ana).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Anita\",\"apellidos\":\"Ruiz\",\"telefono\":\"3222222222\",\"correo\":\"anita@ph.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombres").value("Anita"))
                .andExpect(jsonPath("$.telefono").value("3222222222"));
        mvc.perform(get("/api/personas/" + personaId).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.correo").value("anita@ph.com"));

        mvc.perform(get("/api/perfil").header("Authorization", "Bearer " + admin)).andExpect(status().isForbidden());
        mvc.perform(get("/api/perfil").header("Authorization", "Bearer " + vigilante)).andExpect(status().isForbidden());
        mvc.perform(get("/api/perfil")).andExpect(status().isUnauthorized());
    }

    // ---------- utilidades ----------

    private ResultActions crearPersona(String documento) throws Exception {
        return enviarPost("/api/personas", admin, "{\"tipoPersonaId\":" + tipoResidente + ",\"tipoDocumentoId\":" + tipoDocumento
                + ",\"numeroDocumento\":\"" + documento + "\",\"nombres\":\"Ana\",\"apellidos\":\"Ruiz\","
                + "\"telefono\":\"3000000000\",\"correo\":\"ana@ph.com\",\"autorizaTratamientoDatos\":true}");
    }

    private String inmueble(String torre, String numero, int piso, Long tipoId) {
        return "{\"torreBloque\":\"" + torre + "\",\"numero\":\"" + numero + "\",\"piso\":" + piso + ",\"tipoId\":" + tipoId + "}";
    }

    private String vehiculoJson(Long personaId, Long inmuebleId, String placa) {
        return "{\"personaId\":" + personaId + ",\"inmuebleId\":" + inmuebleId + ",\"placa\":\"" + placa
                + "\",\"tipoId\":" + tipoVehiculo + ",\"marca\":\"Yamaha\",\"color\":\"Negro\"}";
    }

    private Long catalogo(ETipoCatalogo tipo, String nombre) {
        return catalogos.save(ValorCatalogo.builder().tipo(tipo).nombre(nombre).build()).getId();
    }

    private void cuenta(String correo, ERolCuenta rol) {
        cuentas.save(CuentaUsuario.builder().correoAcceso(correo).hashContrasena(codificador.encode(CLAVE)).rol(rol).build());
    }

    private String token(String correo) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.accessToken");
    }

    private ResultActions enviarPost(String ruta, String token, String json) throws Exception {
        return mvc.perform(post(ruta).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions enviarPatch(String ruta, String token, String json) throws Exception {
        return mvc.perform(patch(ruta).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private Long id(ResultActions accion) throws Exception {
        Number id = JsonPath.read(accion.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
