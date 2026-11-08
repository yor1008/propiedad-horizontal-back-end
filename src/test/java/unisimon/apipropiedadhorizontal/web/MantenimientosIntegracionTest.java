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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.domain.model.Inmueble;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.TipoPersona;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;
import unisimon.apipropiedadhorizontal.repository.InmuebleRepository;
import unisimon.apipropiedadhorizontal.repository.MantenimientoRepository;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.repository.ReservaRepository;
import unisimon.apipropiedadhorizontal.repository.TipoPersonaRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.repository.ZonaComunRepository;

import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mantenimientos (CU-028) y su coherencia con las reservas de zonas comunes. */
@SpringBootTest
@AutoConfigureMockMvc
class MantenimientosIntegracionTest {

    private static final String CLAVE = "Contrasena-segura-1";
    private static final String HORARIOS = Stream.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO")
            .map(d -> "{\"diaSemana\":\"" + d + "\",\"horaInicio\":\"08:00\",\"horaFin\":\"22:00\"}")
            .collect(Collectors.joining(",", "[", "]"));

    @Autowired MockMvc mvc;
    @Autowired unisimon.apipropiedadhorizontal.repository.NotificacionRepository notificaciones;
    @Autowired CuentaUsuarioRepository cuentas;
    @Autowired MantenimientoRepository mantenimientos;
    @Autowired ReservaRepository reservas;
    @Autowired ZonaComunRepository zonas;
    @Autowired PersonaRepository personas;
    @Autowired InmuebleRepository inmuebles;
    @Autowired TipoPersonaRepository tiposPersona;
    @Autowired ValorCatalogoRepository catalogos;
    @Autowired PasswordEncoder codificador;

    final LocalDate dia = LocalDate.now().plusDays(3);
    Long tipoMantenimiento;
    String admin;
    String vigilante;
    String residente;

    @BeforeEach
    void preparar() throws Exception {
        limpiar();
        ValorCatalogo documento = catalogos.save(ValorCatalogo.builder().tipo(ETipoCatalogo.TIPO_DOCUMENTO).nombre("Cedula").build());
        ValorCatalogo tipoInmueble = catalogos.save(ValorCatalogo.builder().tipo(ETipoCatalogo.TIPO_INMUEBLE).nombre("Apartamento").build());
        tipoMantenimiento = catalogos.save(ValorCatalogo.builder().tipo(ETipoCatalogo.TIPO_MANTENIMIENTO).nombre("Preventivo").build()).getId();
        TipoPersona tipo = tiposPersona.save(TipoPersona.builder().nombre("Residente").esResidente(true).build());
        Inmueble inmueble = inmuebles.save(Inmueble.builder().torreBloque("A").numero("101").piso(1).tipo(tipoInmueble).build());
        Persona ana = personas.save(Persona.builder().tipoPersona(tipo).tipoDocumento(documento).numeroDocumento("1001")
                .nombres("Ana").apellidos("Prueba").inmueble(inmueble).fechaInicioVinculo(LocalDate.now().minusDays(1)).build());
        cuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, null);
        cuenta("vig@ph.com", ERolCuenta.VIGILANTE, null);
        cuenta("ana@ph.com", ERolCuenta.PERSONA_VINCULADA, ana);
        admin = token("admin@ph.com");
        vigilante = token("vig@ph.com");
        residente = token("ana@ph.com");
    }

    @AfterEach
    void limpiar() {
        notificaciones.deleteAll();
        mantenimientos.deleteAll();
        reservas.deleteAll();
        zonas.deleteAll();
        cuentas.deleteAll();
        personas.deleteAll();
        inmuebles.deleteAll();
        tiposPersona.deleteAll();
        catalogos.deleteAll();
    }

    @Test
    void mantenimientoSeProgramaValidaYSigueSuCicloDeVida() throws Exception {
        String valido = mantenimiento(null, dia + "T09:00", dia + "T11:00");

        enviar(post("/api/mantenimientos"), vigilante, valido).andExpect(status().isForbidden());
        enviar(post("/api/mantenimientos"), residente, valido).andExpect(status().isForbidden());
        mvc.perform(get("/api/mantenimientos")).andExpect(status().isUnauthorized());

        enviar(post("/api/mantenimientos"), admin, mantenimiento(null, dia + "T11:00", dia + "T09:00")).andExpect(status().isUnprocessableContent());
        enviar(post("/api/mantenimientos"), admin, mantenimiento(null, LocalDate.now().minusDays(2) + "T09:00", null)).andExpect(status().isUnprocessableContent());
        enviar(post("/api/mantenimientos"), admin, valido.replace("\"elementoArea\":\"Ascensor\"", "\"elementoArea\":\" \"")).andExpect(status().isBadRequest());
        enviar(post("/api/mantenimientos"), admin, valido.replace("\"tipoId\":" + tipoMantenimiento, "\"tipoId\":999999")).andExpect(status().isNotFound());

        Long id = id(enviar(post("/api/mantenimientos"), admin, valido)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("Programado"))
                .andExpect(jsonPath("$.tipo").value("Preventivo")).andExpect(jsonPath("$.zonaComunId").doesNotExist()));

        enviar(put("/api/mantenimientos/" + id), admin, "{\"descripcion\":\"Cambio de cables\",\"fechaHoraInicio\":\"" + dia + "T10:00\",\"responsable\":\"Otis\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.descripcion").value("Cambio de cables"))
                .andExpect(jsonPath("$.fechaHoraFin").doesNotExist());

        enviar(patch("/api/mantenimientos/" + id + "/finalizar"), admin, "{}").andExpect(status().isUnprocessableContent());
        mvc.perform(patch("/api/mantenimientos/" + id + "/iniciar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("En curso"));
        enviar(put("/api/mantenimientos/" + id), admin, "{\"fechaHoraInicio\":\"" + dia + "T10:00\"}").andExpect(status().isUnprocessableContent());

        enviar(patch("/api/mantenimientos/" + id + "/finalizar"), admin, "{\"observaciones\":\"Todo en orden\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Completado"))
                .andExpect(jsonPath("$.observaciones").value("Todo en orden"))
                .andExpect(jsonPath("$.fechaHoraFin").isNotEmpty());
        mvc.perform(patch("/api/mantenimientos/" + id + "/iniciar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isUnprocessableContent());
        enviar(patch("/api/mantenimientos/" + id + "/cancelar"), admin, "{\"motivo\":\"x\"}").andExpect(status().isUnprocessableContent());

        mvc.perform(get("/api/mantenimientos").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/mantenimientos/" + id).header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        mvc.perform(get("/api/mantenimientos/999999").header("Authorization", "Bearer " + admin)).andExpect(status().isNotFound());
    }

    @Test
    void cancelarExigeMotivo() throws Exception {
        Long id = id(enviar(post("/api/mantenimientos"), admin, mantenimiento(null, dia + "T09:00", dia + "T11:00")).andExpect(status().isCreated()));
        enviar(patch("/api/mantenimientos/" + id + "/cancelar"), admin, "{}").andExpect(status().isBadRequest());
        enviar(patch("/api/mantenimientos/" + id + "/cancelar"), admin, "{\"motivo\":\"Sin presupuesto\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Cancelado"))
                .andExpect(jsonPath("$.motivoCancelacion").value("Sin presupuesto"));
    }

    @Test
    void mantenimientoDeZonaYReservasSeBloqueanEntreSi() throws Exception {
        Long zona = id(enviar(post("/api/zonas-comunes"), admin, zona()).andExpect(status().isCreated()));
        enviar(post("/api/reservas"), residente, reserva(zona, "10:00", "12:00")).andExpect(status().isCreated());

        // No se programa mantenimiento sobre una reserva vigente...
        enviar(post("/api/mantenimientos"), admin, mantenimiento(zona, dia + "T11:00", dia + "T13:00")).andExpect(status().isConflict());
        // ...pero si en otro horario del mismo dia.
        Long mant = id(enviar(post("/api/mantenimientos"), admin, mantenimiento(zona, dia + "T14:00", dia + "T16:00"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.zonaComun").value("BBQ")));

        // La zona en mantenimiento no se puede reservar en ese horario, y la disponibilidad lo muestra.
        enviar(post("/api/reservas"), residente, reserva(zona, "15:00", "16:00")).andExpect(status().isConflict());
        mvc.perform(get("/api/zonas-comunes/" + zona + "/disponibilidad?fecha=" + dia).header("Authorization", "Bearer " + residente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ocupadas.length()").value(2))
                .andExpect(jsonPath("$.ocupadas[1].estado").value("Mantenimiento"))
                .andExpect(jsonPath("$.ocupadas[1].horaInicio").value("14:00:00"));

        // Cancelado el mantenimiento, el horario vuelve a estar libre.
        enviar(patch("/api/mantenimientos/" + mant + "/cancelar"), admin, "{\"motivo\":\"Reprogramado\"}").andExpect(status().isOk());
        enviar(post("/api/reservas"), residente, reserva(zona, "15:00", "16:00")).andExpect(status().isCreated());
    }

    // ---------- utilidades ----------

    private String mantenimiento(Long zonaId, String inicio, String fin) {
        return "{" + (zonaId == null ? "" : "\"zonaComunId\":" + zonaId + ",") + "\"tipoId\":" + tipoMantenimiento
                + ",\"elementoArea\":\"Ascensor\",\"descripcion\":\"Revision\",\"fechaHoraInicio\":\"" + inicio + "\""
                + (fin == null ? "" : ",\"fechaHoraFin\":\"" + fin + "\"") + ",\"responsable\":\"Otis\"}";
    }

    private String zona() {
        return "{\"nombre\":\"BBQ\",\"descripcion\":\"d\",\"capacidadMaxima\":20,\"tipoUso\":\"EXCLUSIVO\","
                + "\"requiereAprobacion\":false,\"duracionMaximaMinutos\":240,\"anticipacionMinimaHoras\":1,"
                + "\"anticipacionMaximaDias\":30,\"limiteCancelacionHoras\":0,\"maxReservasPorInmueble\":0,"
                + "\"periodoLimite\":null,\"horarios\":" + HORARIOS + "}";
    }

    private String reserva(Long zonaId, String inicio, String fin) {
        return "{\"zonaId\":" + zonaId + ",\"fecha\":\"" + dia + "\",\"horaInicio\":\"" + inicio + "\",\"horaFin\":\""
                + fin + "\",\"numeroAsistentes\":5,\"motivo\":\"Cumpleanos\"}";
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

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String token, String json) throws Exception {
        return mvc.perform(peticion.header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private Long id(ResultActions accion) throws Exception {
        Number id = JsonPath.read(accion.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    // ---------- borrado (solo administrador; sin historia) ----------

    @Test
    void mantenimientoProgramadoSeBorraYElQueYaAvanzoNo() throws Exception {
        Long programado = id(enviar(post("/api/mantenimientos"), admin, mantenimiento(null, dia + "T09:00", dia + "T11:00")).andExpect(status().isCreated()));
        Long enCurso = id(enviar(post("/api/mantenimientos"), admin, mantenimiento(null, dia + "T13:00", dia + "T15:00")).andExpect(status().isCreated()));
        mvc.perform(patch("/api/mantenimientos/" + enCurso + "/iniciar").header("Authorization", "Bearer " + admin)).andExpect(status().isOk());

        borrar("/api/mantenimientos/" + programado, vigilante).andExpect(status().isForbidden());
        borrar("/api/mantenimientos/" + programado, admin).andExpect(status().isNoContent());
        borrar("/api/mantenimientos/" + programado, admin).andExpect(status().isNotFound());
        borrar("/api/mantenimientos/" + enCurso, admin).andExpect(status().isConflict());
    }

    private ResultActions borrar(String ruta, String token) throws Exception {
        return mvc.perform(delete(ruta).header("Authorization", "Bearer " + token));
    }
}
