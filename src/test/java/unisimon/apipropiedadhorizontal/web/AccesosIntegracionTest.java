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
import unisimon.apipropiedadhorizontal.domain.enums.ECategoriaInmueble;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.domain.model.Inmueble;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.TipoPersona;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;
import unisimon.apipropiedadhorizontal.repository.InmuebleRepository;
import unisimon.apipropiedadhorizontal.repository.NotificacionRepository;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.repository.PreRegistroRepository;
import unisimon.apipropiedadhorizontal.repository.RegistroVisitaRepository;
import unisimon.apipropiedadhorizontal.repository.TipoPersonaRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.repository.VisitanteRepository;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seguridad (CU-015 a CU-021), notificaciones (CU-004) e inmuebles del propietario (CU-011). */
@SpringBootTest
@AutoConfigureMockMvc
class AccesosIntegracionTest {

    private static final String CLAVE = "Contrasena-segura-1";

    @Autowired MockMvc mvc;
    @Autowired CuentaUsuarioRepository cuentas;
    @Autowired NotificacionRepository notificaciones;
    @Autowired RegistroVisitaRepository registros;
    @Autowired PreRegistroRepository preRegistros;
    @Autowired VisitanteRepository visitantes;
    @Autowired PersonaRepository personas;
    @Autowired InmuebleRepository inmuebles;
    @Autowired TipoPersonaRepository tiposPersona;
    @Autowired ValorCatalogoRepository catalogos;
    @Autowired PasswordEncoder codificador;

    final LocalDate hoy = LocalDate.now();
    Long inmuebleId;
    Long anaId;
    Long luisId;
    Long tipoVehiculo;
    Long parqueadero1;
    Long parqueadero2;
    Long parqueaderoDeInmueble;
    String admin;
    String vigilante;
    String ana;
    String luis;

    @BeforeEach
    void preparar() throws Exception {
        limpiar();
        ValorCatalogo documento = catalogos.save(ValorCatalogo.builder().tipo(ETipoCatalogo.TIPO_DOCUMENTO).nombre("Cedula").build());
        ValorCatalogo tipoInmueble = catalogos.save(ValorCatalogo.builder().tipo(ETipoCatalogo.TIPO_INMUEBLE).nombre("Apartamento").build());
        tipoVehiculo = catalogos.save(ValorCatalogo.builder().tipo(ETipoCatalogo.TIPO_VEHICULO).nombre("Carro").build()).getId();
        TipoPersona residente = tiposPersona.save(TipoPersona.builder().nombre("Residente").esResidente(true).build());
        TipoPersona propietario = tiposPersona.save(TipoPersona.builder().nombre("Propietario").esPropietario(true).build());
        Inmueble apto = inmuebles.save(Inmueble.builder().torreBloque("A").numero("101").piso(1).tipo(tipoInmueble).build());
        inmuebleId = apto.getId();
        parqueadero1 = parqueadero(tipoInmueble, "1", null);
        parqueadero2 = parqueadero(tipoInmueble, "2", null);
        parqueaderoDeInmueble = parqueadero(tipoInmueble, "3", apto);
        Persona personaAna = persona(residente, documento, apto, "1001", "Ana");
        Persona personaLuis = persona(propietario, documento, apto, "1002", "Luis");
        anaId = personaAna.getId();
        luisId = personaLuis.getId();
        cuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, null);
        cuenta("vig@ph.com", ERolCuenta.VIGILANTE, null);
        cuenta("ana@ph.com", ERolCuenta.PERSONA_VINCULADA, personaAna);
        cuenta("luis@ph.com", ERolCuenta.PERSONA_VINCULADA, personaLuis);
        admin = token("admin@ph.com");
        vigilante = token("vig@ph.com");
        ana = token("ana@ph.com");
        luis = token("luis@ph.com");
    }

    @AfterEach
    void limpiar() {
        notificaciones.deleteAll();
        registros.deleteAll();
        preRegistros.deleteAll();
        visitantes.deleteAll();
        cuentas.deleteAll();
        personas.deleteAll();
        inmuebles.deleteAll(inmuebles.findByCategoria(ECategoriaInmueble.PARQUEADERO));
        inmuebles.deleteAll();
        tiposPersona.deleteAll();
        catalogos.deleteAll();
    }

    // ---------- CU-015 pre-registros ----------

    @Test
    void residenteCreaListaYCancelaPreRegistros() throws Exception {
        enviar(post("/api/pre-registros"), luis, preRegistro("9001", hoy.toString())).andExpect(status().isForbidden());
        enviar(post("/api/pre-registros"), vigilante, preRegistro("9001", hoy.toString())).andExpect(status().isForbidden());
        mvc.perform(get("/api/pre-registros/mios")).andExpect(status().isUnauthorized());

        enviar(post("/api/pre-registros"), ana, preRegistro("9001", hoy.minusDays(1).toString())).andExpect(status().isUnprocessableContent());
        enviar(post("/api/pre-registros"), ana, preRegistro("9001", null)).andExpect(status().isUnprocessableContent());
        enviar(post("/api/pre-registros"), ana, preRegistro("9001", hoy.toString()).replace("\"tipoVehiculoId\":" + tipoVehiculo, "\"tipoVehiculoId\":999999"))
                .andExpect(status().isNotFound());
        enviar(post("/api/pre-registros"), ana, "{\"numeroDocumento\":\"9001\"}").andExpect(status().isBadRequest());

        Long id = id(enviar(post("/api/pre-registros"), ana, preRegistro("9001", hoy.toString()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.vigente").value(true))
                .andExpect(jsonPath("$.placaVehiculo").value("ABC123")).andExpect(jsonPath("$.inmueble").value("A-101"))
                .andExpect(jsonPath("$.visitante").value("Pedro Perez")));
        enviar(post("/api/pre-registros"), ana, preRegistro("9001", hoy.toString())).andExpect(status().isConflict());
        // Permanente: no lleva fecha y no choca con el de fecha especifica.
        enviar(post("/api/pre-registros"), ana, "{\"nombreCompleto\":\"Pedro Perez\",\"numeroDocumento\":\"9001\",\"tipoVigencia\":\"PERMANENTE\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.fechaPrevista").doesNotExist());

        mvc.perform(get("/api/pre-registros/mios").header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        enviar(patch("/api/pre-registros/" + id + "/cancelar"), ana, "")
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false)).andExpect(jsonPath("$.vigente").value(false));
        enviar(patch("/api/pre-registros/" + id + "/cancelar"), ana, "").andExpect(status().isUnprocessableContent());
        enviar(patch("/api/pre-registros/999999/cancelar"), ana, "").andExpect(status().isNotFound());
    }

    // ---------- CU-016, 018, 019, 020, 021 con pre-registro ----------

    @Test
    void visitanteConPreRegistroEntraTomaParqueaderoYSale() throws Exception {
        Long pre = id(enviar(post("/api/pre-registros"), ana, preRegistro("9001", hoy.toString())).andExpect(status().isCreated()));

        mvc.perform(get("/api/accesos/pre-registros?documento=9001").header("Authorization", "Bearer " + vigilante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/accesos/pre-registros?documento=0000").header("Authorization", "Bearer " + vigilante))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/accesos/pre-registros?documento=9001").header("Authorization", "Bearer " + ana)).andExpect(status().isForbidden());

        enviar(post("/api/accesos/con-preregistro"), ana, "{\"preRegistroId\":" + pre + "}").andExpect(status().isForbidden());
        Long visita = id(enviar(post("/api/accesos/con-preregistro"), vigilante, "{\"preRegistroId\":" + pre + "}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("Dentro"))
                .andExpect(jsonPath("$.conPreRegistro").value(true)).andExpect(jsonPath("$.dentro").value(true))
                .andExpect(jsonPath("$.tipoVehiculo").value("Carro")));
        // De fecha especifica: queda usado, no sirve otra vez.
        enviar(post("/api/accesos/con-preregistro"), vigilante, "{\"preRegistroId\":" + pre + "}").andExpect(status().isUnprocessableContent());
        enviar(post("/api/accesos/con-preregistro"), vigilante, "{\"preRegistroId\":999999}").andExpect(status().isNotFound());

        // La residente recibe una notificacion y solo ella puede leerla.
        mvc.perform(get("/api/notificaciones/mias/conteo").header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$.noLeidas").value(1));
        mvc.perform(get("/api/notificaciones/mias/conteo").header("Authorization", "Bearer " + luis))
                .andExpect(jsonPath("$.noLeidas").value(0));
        Long notificacion = id(mvc.perform(get("/api/notificaciones/mias?soloNoLeidas=true").header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$[0].evento").value("INGRESO_VISITANTE")), "$[0].id");
        enviar(patch("/api/notificaciones/" + notificacion + "/leer"), luis, "").andExpect(status().isNotFound());
        enviar(patch("/api/notificaciones/" + notificacion + "/leer"), ana, "")
                .andExpect(status().isOk()).andExpect(jsonPath("$.leida").value(true));
        mvc.perform(get("/api/notificaciones/mias/conteo").header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$.noLeidas").value(0));
        mvc.perform(get("/api/notificaciones/mias").header("Authorization", "Bearer " + vigilante)).andExpect(status().isForbidden());

        // Visitantes dentro y parqueadero (CU-018): solo los de visitantes, uno por visita.
        mvc.perform(get("/api/accesos/dentro").header("Authorization", "Bearer " + vigilante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/accesos/parqueaderos-disponibles").header("Authorization", "Bearer " + vigilante))
                .andExpect(jsonPath("$.length()").value(2));
        enviar(patch("/api/accesos/" + visita + "/parqueadero"), vigilante, "{\"parqueaderoId\":" + parqueaderoDeInmueble + "}")
                .andExpect(status().isUnprocessableContent());
        enviar(patch("/api/accesos/" + visita + "/parqueadero"), vigilante, "{\"parqueaderoId\":" + parqueadero1 + "}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.parqueadero").value("P-1"));

        Long otraVisita = id(enviar(post("/api/accesos/sin-preregistro"), vigilante, sinPreRegistro("9002", "APRUEBA", null))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("Dentro"))
                .andExpect(jsonPath("$.conPreRegistro").value(false)).andExpect(jsonPath("$.residenteContactado").value("Ana Prueba")));
        enviar(patch("/api/accesos/" + otraVisita + "/parqueadero"), vigilante, "{\"parqueaderoId\":" + parqueadero1 + "}")
                .andExpect(status().isConflict());
        enviar(patch("/api/accesos/" + otraVisita + "/parqueadero"), vigilante, "{\"parqueaderoId\":" + parqueadero2 + "}").andExpect(status().isOk());
        mvc.perform(get("/api/accesos/parqueaderos-disponibles").header("Authorization", "Bearer " + vigilante))
                .andExpect(jsonPath("$.length()").value(0));

        // El mismo visitante no puede entrar dos veces sin salir.
        enviar(post("/api/accesos/sin-preregistro"), vigilante, sinPreRegistro("9002", "APRUEBA", null)).andExpect(status().isConflict());

        // Salida (CU-019): libera el parqueadero y no se repite.
        enviar(patch("/api/accesos/" + visita + "/salida"), vigilante, "")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("Salio")).andExpect(jsonPath("$.dentro").value(false));
        enviar(patch("/api/accesos/" + visita + "/salida"), vigilante, "").andExpect(status().isUnprocessableContent());
        enviar(patch("/api/accesos/" + visita + "/parqueadero"), vigilante, "{\"parqueaderoId\":" + parqueadero1 + "}")
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/accesos/parqueaderos-disponibles").header("Authorization", "Bearer " + vigilante))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].numero").value("1"));
        mvc.perform(get("/api/accesos/dentro").header("Authorization", "Bearer " + vigilante))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- CU-017 sin pre-registro ----------

    @Test
    void ingresoSinPreRegistroPuedeSerNegadoYQuedaEnElHistorial() throws Exception {
        enviar(post("/api/accesos/sin-preregistro"), vigilante, sinPreRegistro("9003", "NO_CONTESTA", null))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("Negado"))
                .andExpect(jsonPath("$.ingresoAutorizado").value(false))
                .andExpect(jsonPath("$.motivoNegacion").value("El residente no contesto"))
                .andExpect(jsonPath("$.fechaHoraEntrada").doesNotExist());
        enviar(post("/api/accesos/sin-preregistro"), vigilante, sinPreRegistro("9004", "NO_APRUEBA", "No espero a nadie"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.motivoNegacion").value("No espero a nadie"));
        // Negado no ocupa lugar dentro ni genera notificacion.
        mvc.perform(get("/api/accesos/dentro").header("Authorization", "Bearer " + vigilante)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/notificaciones/mias/conteo").header("Authorization", "Bearer " + ana)).andExpect(jsonPath("$.noLeidas").value(0));
        // Quien fue negado puede volver a intentar.
        enviar(post("/api/accesos/sin-preregistro"), vigilante, sinPreRegistro("9003", "APRUEBA", null)).andExpect(status().isCreated());

        // Reglas: el contactado debe ser residente vigente del inmueble (Luis es solo propietario).
        enviar(post("/api/accesos/sin-preregistro"), vigilante, sinPreRegistro("9005", "APRUEBA", null).replace("\"residenteContactadoId\":" + anaId, "\"residenteContactadoId\":" + luisId))
                .andExpect(status().isUnprocessableContent());
        enviar(post("/api/accesos/sin-preregistro"), vigilante, sinPreRegistro("9005", "APRUEBA", null).replace("\"inmuebleId\":" + inmuebleId, "\"inmuebleId\":999999"))
                .andExpect(status().isNotFound());
        enviar(post("/api/accesos/sin-preregistro"), vigilante, sinPreRegistro("9005", "APRUEBA", null).replace("\"inmuebleId\":" + inmuebleId, "\"inmuebleId\":" + parqueadero1))
                .andExpect(status().isUnprocessableContent());
        enviar(post("/api/accesos/sin-preregistro"), admin, sinPreRegistro("9005", "APRUEBA", null)).andExpect(status().isForbidden());
        enviar(post("/api/accesos/sin-preregistro"), vigilante, "{\"numeroDocumento\":\"9005\"}").andExpect(status().isBadRequest());

        // Directorio de porteria: la unidad con sus residentes (Luis no es residente).
        mvc.perform(get("/api/accesos/inmuebles").header("Authorization", "Bearer " + vigilante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numero").value("101")).andExpect(jsonPath("$[0].residentes.length()").value(1))
                .andExpect(jsonPath("$[0].residentes[0].nombre").value("Ana Prueba"));
    }

    // ---------- CU-021 historial ----------

    @Test
    void historialLoVenAdministradorYVigilanteConFiltros() throws Exception {
        enviar(post("/api/accesos/sin-preregistro"), vigilante, sinPreRegistro("9006", "APRUEBA", null)).andExpect(status().isCreated());
        enviar(post("/api/accesos/sin-preregistro"), vigilante, sinPreRegistro("9007", "NO_APRUEBA", null)).andExpect(status().isCreated());

        for (String token : new String[]{admin, vigilante}) {
            mvc.perform(get("/api/accesos/historial").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].numeroDocumento").value("9007"));
        }
        mvc.perform(get("/api/accesos/historial?documento=9006").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].estado").value("Dentro"));
        mvc.perform(get("/api/accesos/historial?desde=" + hoy + "&hasta=" + hoy).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/accesos/historial?desde=" + hoy.plusDays(1)).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/accesos/historial?hasta=" + hoy.minusDays(1)).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/accesos/historial?desde=" + hoy + "&hasta=" + hoy.minusDays(1)).header("Authorization", "Bearer " + admin))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/accesos/historial?desde=ayer").header("Authorization", "Bearer " + admin)).andExpect(status().isBadRequest());

        mvc.perform(get("/api/accesos/historial").header("Authorization", "Bearer " + ana)).andExpect(status().isForbidden());
        mvc.perform(get("/api/accesos/historial")).andExpect(status().isUnauthorized());
        // Salida y dentro son solo del vigilante.
        mvc.perform(get("/api/accesos/dentro").header("Authorization", "Bearer " + admin)).andExpect(status().isForbidden());
    }

    // ---------- CU-004 leer todas / CU-011 propietario ----------

    @Test
    void marcarTodasLasNotificacionesComoLeidas() throws Exception {
        enviar(post("/api/accesos/sin-preregistro"), vigilante, sinPreRegistro("9008", "APRUEBA", null)).andExpect(status().isCreated());
        enviar(patch("/api/accesos/" + id(mvc.perform(get("/api/accesos/dentro").header("Authorization", "Bearer " + vigilante)), "$[0].id") + "/salida"), vigilante, "");
        enviar(post("/api/accesos/sin-preregistro"), vigilante, sinPreRegistro("9008", "APRUEBA", null)).andExpect(status().isCreated());
        mvc.perform(get("/api/notificaciones/mias/conteo").header("Authorization", "Bearer " + ana)).andExpect(jsonPath("$.noLeidas").value(2));
        enviar(patch("/api/notificaciones/leer-todas"), ana, "").andExpect(status().isOk()).andExpect(jsonPath("$.noLeidas").value(0));
        mvc.perform(get("/api/notificaciones/mias").header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].leida").value(true));
        mvc.perform(get("/api/notificaciones/mias?soloNoLeidas=true").header("Authorization", "Bearer " + ana))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void propietarioVeSusInmueblesConSusParqueaderos() throws Exception {
        mvc.perform(get("/api/mis-inmuebles").header("Authorization", "Bearer " + luis))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numero").value("101")).andExpect(jsonPath("$[0].ocupado").value(false))
                .andExpect(jsonPath("$[0].parqueaderos.length()").value(1))
                .andExpect(jsonPath("$[0].parqueaderos[0].numero").value("3"));
        mvc.perform(get("/api/mis-inmuebles").header("Authorization", "Bearer " + ana)).andExpect(status().isForbidden());
        mvc.perform(get("/api/mis-inmuebles").header("Authorization", "Bearer " + admin)).andExpect(status().isForbidden());
        mvc.perform(get("/api/mis-inmuebles")).andExpect(status().isUnauthorized());
    }

    // ---------- utilidades ----------

    private String preRegistro(String documento, String fecha) {
        return "{\"nombreCompleto\":\"Pedro Perez\",\"numeroDocumento\":\"" + documento
                + "\",\"tipoVigencia\":\"FECHA_ESPECIFICA\"" + (fecha == null ? "" : ",\"fechaPrevista\":\"" + fecha + "\"")
                + ",\"motivo\":\"Visita\",\"placaVehiculo\":\"abc123\",\"tipoVehiculoId\":" + tipoVehiculo + "}";
    }

    private String sinPreRegistro(String documento, String respuesta, String motivo) {
        return "{\"nombreCompleto\":\"Maria Lopez\",\"numeroDocumento\":\"" + documento + "\",\"inmuebleId\":" + inmuebleId
                + ",\"residenteContactadoId\":" + anaId + ",\"respuestaResidente\":\"" + respuesta + "\""
                + (motivo == null ? "" : ",\"motivoNegacion\":\"" + motivo + "\"") + "}";
    }

    private Long parqueadero(ValorCatalogo tipo, String numero, Inmueble asociado) {
        return inmuebles.save(Inmueble.builder().torreBloque("P").numero(numero).piso(0).tipo(tipo)
                .categoria(ECategoriaInmueble.PARQUEADERO).inmuebleAsociado(asociado).build()).getId();
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
        return id(accion, "$.id");
    }

    private Long id(ResultActions accion, String ruta) throws Exception {
        Number id = JsonPath.read(accion.andReturn().getResponse().getContentAsString(), ruta);
        return id.longValue();
    }

    // ---------- borrado de pre-registros (solo administrador; sin visitas) ----------

    @Test
    void preRegistroSinVisitasSeBorraYElUsadoNo() throws Exception {
        Long libre = id(enviar(post("/api/pre-registros"), ana, preRegistro("9101", hoy.toString())).andExpect(status().isCreated()));
        Long usado = id(enviar(post("/api/pre-registros"), ana, preRegistro("9102", hoy.toString())).andExpect(status().isCreated()));
        enviar(post("/api/accesos/con-preregistro"), vigilante, "{\"preRegistroId\":" + usado + "}").andExpect(status().isCreated());

        borrar("/api/pre-registros/" + libre, ana).andExpect(status().isForbidden());
        borrar("/api/pre-registros/" + libre, admin).andExpect(status().isNoContent());
        borrar("/api/pre-registros/" + libre, admin).andExpect(status().isNotFound());
        borrar("/api/pre-registros/" + usado, admin).andExpect(status().isConflict());
    }

    private ResultActions borrar(String ruta, String token) throws Exception {
        return mvc.perform(delete(ruta).header("Authorization", "Bearer " + token));
    }
}
