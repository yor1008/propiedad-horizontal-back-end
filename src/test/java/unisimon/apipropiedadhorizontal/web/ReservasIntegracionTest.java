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

/** Zonas comunes y reservas (CU-024, 025, 026, 027). */
@SpringBootTest
@AutoConfigureMockMvc
class ReservasIntegracionTest {

    private static final String CLAVE = "Contrasena-segura-1";
    private static final String HORARIOS = Stream.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO")
            .map(d -> "{\"diaSemana\":\"" + d + "\",\"horaInicio\":\"08:00\",\"horaFin\":\"22:00\"}")
            .collect(Collectors.joining(",", "[", "]"));

    @Autowired MockMvc mvc;
    @Autowired unisimon.apipropiedadhorizontal.repository.NotificacionRepository notificaciones;
    @Autowired CuentaUsuarioRepository cuentas;
    @Autowired ReservaRepository reservas;
    @Autowired ZonaComunRepository zonas;
    @Autowired PersonaRepository personas;
    @Autowired InmuebleRepository inmuebles;
    @Autowired TipoPersonaRepository tiposPersona;
    @Autowired ValorCatalogoRepository catalogos;
    @Autowired PasswordEncoder codificador;

    final String fecha = LocalDate.now().plusDays(3).toString();
    String admin;
    String vigilante;
    String residente;
    String otroResidente;

    @BeforeEach
    void preparar() throws Exception {
        limpiar();
        ValorCatalogo documento = catalogos.save(ValorCatalogo.builder().tipo(ETipoCatalogo.TIPO_DOCUMENTO).nombre("Cedula").build());
        ValorCatalogo tipoInmueble = catalogos.save(ValorCatalogo.builder().tipo(ETipoCatalogo.TIPO_INMUEBLE).nombre("Apartamento").build());
        TipoPersona tipo = tiposPersona.save(TipoPersona.builder().nombre("Residente").esResidente(true).build());
        Inmueble inmueble = inmuebles.save(Inmueble.builder().torreBloque("A").numero("101").piso(1).tipo(tipoInmueble).build());
        Persona ana = persona(tipo, documento, inmueble, "1001", "Ana");
        Persona luis = persona(tipo, documento, inmueble, "1002", "Luis");
        cuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, null);
        cuenta("vig@ph.com", ERolCuenta.VIGILANTE, null);
        cuenta("ana@ph.com", ERolCuenta.PERSONA_VINCULADA, ana);
        cuenta("luis@ph.com", ERolCuenta.PERSONA_VINCULADA, luis);
        admin = token("admin@ph.com");
        vigilante = token("vig@ph.com");
        residente = token("ana@ph.com");
        otroResidente = token("luis@ph.com");
    }

    @AfterEach
    void limpiar() {
        notificaciones.deleteAll();
        reservas.deleteAll();
        zonas.deleteAll();
        cuentas.deleteAll();
        personas.deleteAll();
        inmuebles.deleteAll();
        tiposPersona.deleteAll();
        catalogos.deleteAll();
    }

    @Test
    void zonaSeValidaSeCreaSeActualizaYSeDesactiva() throws Exception {
        enviar(post("/api/zonas-comunes"), admin, zona("Salon", false, 0, "null", "[]")).andExpect(status().isBadRequest());
        enviar(post("/api/zonas-comunes"), admin, zona("Salon", false, 2, "null", HORARIOS)).andExpect(status().isUnprocessableContent());
        enviar(post("/api/zonas-comunes"), admin, zona("Salon", false, 0, "null",
                "[{\"diaSemana\":\"LUNES\",\"horaInicio\":\"10:00\",\"horaFin\":\"09:00\"}]")).andExpect(status().isUnprocessableContent());

        Long id = id(enviar(post("/api/zonas-comunes"), admin, zona("Salon", false, 0, "null", HORARIOS))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.horarios.length()").value(7)));
        enviar(post("/api/zonas-comunes"), admin, zona("salon", false, 0, "null", HORARIOS)).andExpect(status().isConflict());

        enviar(put("/api/zonas-comunes/" + id), admin, zona("Salon Social", true, 0, "null", HORARIOS))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Salon Social"))
                .andExpect(jsonPath("$.requiereAprobacion").value(true));

        mvc.perform(get("/api/zonas-comunes").header("Authorization", "Bearer " + residente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/zonas-comunes/todas").header("Authorization", "Bearer " + residente)).andExpect(status().isForbidden());
        mvc.perform(get("/api/zonas-comunes")).andExpect(status().isUnauthorized());

        enviar(patch("/api/zonas-comunes/" + id + "/desactivar"), admin, "{\"motivo\":\"Remodelacion\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(false));
        mvc.perform(get("/api/zonas-comunes").header("Authorization", "Bearer " + residente))
                .andExpect(jsonPath("$.length()").value(0));
        enviar(post("/api/reservas"), residente, reserva(id, "10:00", "12:00", 5)).andExpect(status().isUnprocessableContent());
    }

    @Test
    void reservaSinAprobacionSeApruebaSolaRespetaReglasYSeCancela() throws Exception {
        Long zona = id(enviar(post("/api/zonas-comunes"), admin, zona("BBQ", false, 0, "null", HORARIOS)).andExpect(status().isCreated()));

        Long reserva = id(enviar(post("/api/reservas"), residente, reserva(zona, "10:00", "12:00", 10))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("Aprobada"))
                .andExpect(jsonPath("$.inmueble").value("A-101")));

        enviar(post("/api/reservas"), residente, reserva(zona, "11:00", "13:00", 4)).andExpect(status().isConflict());
        enviar(post("/api/reservas"), residente, reserva(zona, "05:00", "06:00", 4)).andExpect(status().isUnprocessableContent());
        enviar(post("/api/reservas"), residente, reserva(zona, "14:00", "15:00", 999)).andExpect(status().isUnprocessableContent());
        enviar(post("/api/reservas"), residente, reserva(zona, "16:00", "15:00", 3)).andExpect(status().isUnprocessableContent());
        enviar(post("/api/reservas"), admin, reserva(zona, "14:00", "15:00", 3)).andExpect(status().isForbidden());
        enviar(post("/api/reservas"), vigilante, reserva(zona, "14:00", "15:00", 3)).andExpect(status().isForbidden());

        mvc.perform(get("/api/zonas-comunes/" + zona + "/disponibilidad?fecha=" + fecha).header("Authorization", "Bearer " + residente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.horarios.length()").value(1))
                .andExpect(jsonPath("$.ocupadas.length()").value(1));
        mvc.perform(get("/api/zonas-comunes/" + zona + "/disponibilidad?fecha=manana").header("Authorization", "Bearer " + residente))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/reservas/mias").header("Authorization", "Bearer " + residente))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].puedeCancelar").value(true));
        mvc.perform(get("/api/reservas/mias").header("Authorization", "Bearer " + otroResidente))
                .andExpect(jsonPath("$.length()").value(0));

        // Una reserva ajena no se ve ni se cancela (404, sin revelar que existe).
        enviar(patch("/api/reservas/" + reserva + "/cancelar"), otroResidente, "{\"motivo\":\"x\"}").andExpect(status().isNotFound());
        enviar(patch("/api/reservas/" + reserva + "/cancelar"), residente, "{\"motivo\":\" \"}").andExpect(status().isBadRequest());
        enviar(patch("/api/reservas/" + reserva + "/cancelar"), residente, "{\"motivo\":\"Cambio de planes\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Cancelada"))
                .andExpect(jsonPath("$.motivoRechazoCancelacion").value("Cambio de planes"));
        enviar(patch("/api/reservas/" + reserva + "/cancelar"), residente, "{\"motivo\":\"otra vez\"}")
                .andExpect(status().isUnprocessableContent());
        // El horario cancelado queda libre.
        enviar(post("/api/reservas"), residente, reserva(zona, "10:00", "12:00", 10)).andExpect(status().isCreated());
    }

    @Test
    void reservaConAprobacionPasaPorPendienteRechazoAprobacionYFinalizacion() throws Exception {
        Long zona = id(enviar(post("/api/zonas-comunes"), admin, zona("Salon", true, 0, "null", HORARIOS)).andExpect(status().isCreated()));

        Long primera = id(enviar(post("/api/reservas"), residente, reserva(zona, "10:00", "12:00", 10))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("Pendiente")));

        enviar(patch("/api/reservas/" + primera + "/rechazar"), residente, "{\"motivo\":\"x\"}").andExpect(status().isForbidden());
        enviar(patch("/api/reservas/" + primera + "/rechazar"), admin, "{}").andExpect(status().isBadRequest());
        enviar(patch("/api/reservas/" + primera + "/rechazar"), admin, "{\"motivo\":\"Evento del conjunto\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Rechazada"));
        enviar(patch("/api/reservas/" + primera + "/aprobar"), admin, "{}").andExpect(status().isUnprocessableContent());

        Long segunda = id(enviar(post("/api/reservas"), residente, reserva(zona, "10:00", "12:00", 10))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("Pendiente")));
        mvc.perform(patch("/api/reservas/" + segunda + "/aprobar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Aprobada"));
        enviar(patch("/api/reservas/" + segunda + "/finalizar"), admin, "")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Finalizada"));
        enviar(patch("/api/reservas/" + segunda + "/anular"), admin, "{\"motivo\":\"x\"}").andExpect(status().isUnprocessableContent());

        mvc.perform(get("/api/reservas").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/reservas").header("Authorization", "Bearer " + residente)).andExpect(status().isForbidden());
    }

    @Test
    void limitePorInmuebleYPeriodo() throws Exception {
        Long zona = id(enviar(post("/api/zonas-comunes"), admin, zona("Piscina", false, 1, "\"DIA\"", HORARIOS)).andExpect(status().isCreated()));
        enviar(post("/api/reservas"), residente, reserva(zona, "10:00", "11:00", 2)).andExpect(status().isCreated());
        // Mismo inmueble (otra persona), mismo dia, otro horario: supera el maximo de 1 por dia.
        enviar(post("/api/reservas"), otroResidente, reserva(zona, "15:00", "16:00", 2)).andExpect(status().isUnprocessableContent());
    }

    // ---------- utilidades ----------

    private String zona(String nombre, boolean aprobacion, int maxPorInmueble, String periodo, String horarios) {
        return "{\"nombre\":\"" + nombre + "\",\"descripcion\":\"d\",\"capacidadMaxima\":20,\"tipoUso\":\"EXCLUSIVO\","
                + "\"requiereAprobacion\":" + aprobacion + ",\"duracionMaximaMinutos\":240,\"anticipacionMinimaHoras\":1,"
                + "\"anticipacionMaximaDias\":30,\"limiteCancelacionHoras\":0,\"maxReservasPorInmueble\":" + maxPorInmueble
                + ",\"periodoLimite\":" + periodo + ",\"horarios\":" + horarios + "}";
    }

    private String reserva(Long zonaId, String inicio, String fin, int asistentes) {
        return "{\"zonaId\":" + zonaId + ",\"fecha\":\"" + fecha + "\",\"horaInicio\":\"" + inicio + "\",\"horaFin\":\""
                + fin + "\",\"numeroAsistentes\":" + asistentes + ",\"motivo\":\"Cumpleanos\"}";
    }

    private Persona persona(TipoPersona tipo, ValorCatalogo documento, Inmueble inmueble, String numero, String nombre) {
        return personas.save(Persona.builder().tipoPersona(tipo).tipoDocumento(documento).numeroDocumento(numero)
                .nombres(nombre).apellidos("Prueba").inmueble(inmueble)
                .fechaInicioVinculo(LocalDate.now().minusDays(1)).build());
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
    void reservaPendienteSeBorraYLaAprobadaNo() throws Exception {
        Long zona = id(enviar(post("/api/zonas-comunes"), admin, zona("Salon", true, 0, "null", HORARIOS)).andExpect(status().isCreated()));
        Long pendiente = id(enviar(post("/api/reservas"), residente, reserva(zona, "10:00", "12:00", 5)).andExpect(status().isCreated()));
        Long aprobada = id(enviar(post("/api/reservas"), residente, reserva(zona, "14:00", "16:00", 5)).andExpect(status().isCreated()));
        mvc.perform(patch("/api/reservas/" + aprobada + "/aprobar").header("Authorization", "Bearer " + admin)).andExpect(status().isOk());

        borrar("/api/reservas/" + pendiente, residente).andExpect(status().isForbidden());
        borrar("/api/reservas/" + pendiente, admin).andExpect(status().isNoContent());
        borrar("/api/reservas/" + pendiente, admin).andExpect(status().isNotFound());
        borrar("/api/reservas/" + aprobada, admin).andExpect(status().isConflict());
    }

    @Test
    void zonaSinReservasSeBorraConSusHorariosYConReservasNo() throws Exception {
        Long libre = id(enviar(post("/api/zonas-comunes"), admin, zona("Terraza", false, 0, "null", HORARIOS)).andExpect(status().isCreated()));
        Long usada = id(enviar(post("/api/zonas-comunes"), admin, zona("BBQ", false, 0, "null", HORARIOS)).andExpect(status().isCreated()));
        enviar(post("/api/reservas"), residente, reserva(usada, "10:00", "12:00", 5)).andExpect(status().isCreated());

        borrar("/api/zonas-comunes/" + libre, residente).andExpect(status().isForbidden());
        borrar("/api/zonas-comunes/" + libre, admin).andExpect(status().isNoContent());
        borrar("/api/zonas-comunes/" + libre, admin).andExpect(status().isNotFound());
        borrar("/api/zonas-comunes/" + usada, admin).andExpect(status().isConflict());
        // La zona con historia sigue ahi: se desactiva en su lugar.
        mvc.perform(get("/api/zonas-comunes/" + usada).header("Authorization", "Bearer " + residente)).andExpect(status().isOk());
    }

    private ResultActions borrar(String ruta, String token) throws Exception {
        return mvc.perform(delete(ruta).header("Authorization", "Bearer " + token));
    }
}
