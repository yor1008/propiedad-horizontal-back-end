package unisimon.apipropiedadhorizontal.security;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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
import unisimon.apipropiedadhorizontal.repository.TipoPersonaRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SeguridadJwtIntegracionTest {

    private static final String CLAVE = "Contrasena-segura-1";

    @Autowired MockMvc mvc;
    @Autowired CuentaUsuarioRepository cuentas;
    @Autowired PersonaRepository personas;
    @Autowired InmuebleRepository inmuebles;
    @Autowired TipoPersonaRepository tiposPersona;
    @Autowired ValorCatalogoRepository catalogos;
    @Autowired PasswordEncoder codificador;
    @Autowired JwtProperties propiedades;

    @BeforeEach
    void limpiar() {
        cuentas.deleteAll();
        personas.deleteAll();
        inmuebles.deleteAll();
        tiposPersona.deleteAll();
        catalogos.deleteAll();
    }

    // ---------- login (CU-001) ----------

    @Test
    void loginCorrectoEmiteTokenYNoCreaSesion() throws Exception {
        crearCuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, true);

        login("admin@ph.com", CLAVE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void loginNormalizaElCorreo() throws Exception {
        crearCuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, true);
        login("  ADMIN@PH.COM ", CLAVE).andExpect(status().isOk());
    }

    @Test
    void loginFallidoDaElMismoMensajeParaTodosLosCasos() throws Exception {
        crearCuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, true);
        crearCuenta("baja@ph.com", ERolCuenta.VIGILANTE, false);

        for (String[] intento : new String[][]{
                {"admin@ph.com", "otra-clave-xx"},   // clave incorrecta
                {"noexiste@ph.com", CLAVE},          // cuenta inexistente
                {"baja@ph.com", CLAVE}}) {           // cuenta inactiva
            login(intento[0], intento[1])
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensaje").value("Credenciales invalidas"));
        }
    }

    @Test
    void loginConCuerpoInvalidoEs400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- autenticacion de peticiones ----------

    @Test
    void sinTokenEs401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        mvc.perform(get("/api/cuentas")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenManipuladoEs401() throws Exception {
        crearCuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, true);
        String token = tokenDe("admin@ph.com");
        String alterado = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFirmadoConOtraClaveEs401() throws Exception {
        String falso = firmar("otro-secreto-distinto-0123456789abcdef", propiedades.emisor(),
                Instant.now().plusSeconds(300), List.of("ROLE_ADMINISTRADOR"));
        mvc.perform(get("/api/cuentas").header("Authorization", "Bearer " + falso))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoEs401() throws Exception {
        String expirado = firmar(propiedades.secret(), propiedades.emisor(),
                Instant.now().minusSeconds(600), List.of("ROLE_ADMINISTRADOR"));
        mvc.perform(get("/api/cuentas").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeOtroEmisorEs401() throws Exception {
        String ajeno = firmar(propiedades.secret(), "otro-emisor",
                Instant.now().plusSeconds(300), List.of("ROLE_ADMINISTRADOR"));
        mvc.perform(get("/api/cuentas").header("Authorization", "Bearer " + ajeno))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilSeLeeDelTokenSinConsultarLaBaseDeDatos() throws Exception {
        crearCuenta("vig@ph.com", ERolCuenta.VIGILANTE, true);
        String token = tokenDe("vig@ph.com");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("vig@ph.com"))
                .andExpect(jsonPath("$.rol").value("VIGILANTE"))
                .andExpect(jsonPath("$.autoridades[0]").value("ROLE_VIGILANTE"));
    }

    // ---------- autorizacion por etiqueta ----------

    @Test
    void soloAdministradorAccedeAGestionDeCuentas() throws Exception {
        crearCuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, true);
        crearCuenta("vig@ph.com", ERolCuenta.VIGILANTE, true);

        mvc.perform(get("/api/cuentas").header("Authorization", "Bearer " + tokenDe("vig@ph.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));

        mvc.perform(get("/api/cuentas").header("Authorization", "Bearer " + tokenDe("admin@ph.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].hashContrasena").doesNotExist());
    }

    @Test
    void catalogoSeLeeAutenticadoPeroSoloAdminEscribe() throws Exception {
        crearCuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, true);
        crearCuenta("vig@ph.com", ERolCuenta.VIGILANTE, true);
        String admin = "Bearer " + tokenDe("admin@ph.com");
        String vigilante = "Bearer " + tokenDe("vig@ph.com");
        String cuerpo = "{\"tipo\":\"TIPO_VEHICULO\",\"nombre\":\"Moto\"}";

        mvc.perform(post("/api/catalogos").header("Authorization", vigilante)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/catalogos").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/catalogos").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo.replace("Moto", "moto")))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/catalogos/TIPO_VEHICULO").header("Authorization", vigilante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Moto"));
        mvc.perform(get("/api/catalogos/TIPO_VEHICULO/todos").header("Authorization", vigilante))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/catalogos/NO_EXISTE").header("Authorization", vigilante))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogoDesactivadoDejaDeListarseParaUsuariosNormales() throws Exception {
        crearCuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, true);
        String admin = "Bearer " + tokenDe("admin@ph.com");
        ValorCatalogo valor = catalogos.save(ValorCatalogo.builder()
                .tipo(ETipoCatalogo.TIPO_PQRS).nombre("Queja").build());

        mvc.perform(patch("/api/catalogos/{id}/desactivar", valor.getId()).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(get("/api/catalogos/TIPO_PQRS").header("Authorization", admin))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/catalogos/TIPO_PQRS/todos").header("Authorization", admin))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- gestion de cuentas (CU-005) ----------

    @Test
    void crearCuentaValidaReglasDeNegocio() throws Exception {
        crearCuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, true);
        String admin = "Bearer " + tokenDe("admin@ph.com");

        mvc.perform(post("/api/cuentas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"vig@ph.com\",\"contrasena\":\"" + CLAVE + "\",\"rol\":\"VIGILANTE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activa").value(true));
        // duplicado
        mvc.perform(post("/api/cuentas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"VIG@ph.com\",\"contrasena\":\"" + CLAVE + "\",\"rol\":\"VIGILANTE\"}"))
                .andExpect(status().isConflict());
        // persona vinculada sin persona
        mvc.perform(post("/api/cuentas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"p@ph.com\",\"contrasena\":\"" + CLAVE + "\",\"rol\":\"PERSONA_VINCULADA\"}"))
                .andExpect(status().isUnprocessableContent());
        // contrasena corta
        mvc.perform(post("/api/cuentas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"x@ph.com\",\"contrasena\":\"corta\",\"rol\":\"VIGILANTE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void administradorNoPuedeDesactivarseASiMismo() throws Exception {
        CuentaUsuario admin = crearCuenta("admin@ph.com", ERolCuenta.ADMINISTRADOR, true);
        mvc.perform(patch("/api/cuentas/{id}/desactivar", admin.getId())
                        .header("Authorization", "Bearer " + tokenDe("admin@ph.com")))
                .andExpect(status().isUnprocessableContent());
    }

    // ---------- contrasena (CU-002) ----------

    @Test
    void cambiarContrasena() throws Exception {
        crearCuenta("vig@ph.com", ERolCuenta.VIGILANTE, true);
        String token = "Bearer " + tokenDe("vig@ph.com");

        mvc.perform(post("/api/auth/cambiar-contrasena").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"incorrecta-123\",\"nueva\":\"NuevaClave-456\"}"))
                .andExpect(status().isUnprocessableContent());

        mvc.perform(post("/api/auth/cambiar-contrasena").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"" + CLAVE + "\",\"nueva\":\"NuevaClave-456\"}"))
                .andExpect(status().isNoContent());

        login("vig@ph.com", CLAVE).andExpect(status().isUnauthorized());
        login("vig@ph.com", "NuevaClave-456").andExpect(status().isOk());
    }

    // ---------- autoridades derivadas de TipoPersona ----------

    @Test
    void personaVinculadaResidenteRecibeRolesDerivados() throws Exception {
        TipoPersona residente = tiposPersona.save(TipoPersona.builder()
                .nombre("Residente").esResidente(true).esPropietario(false).build());
        ValorCatalogo doc = catalogos.save(ValorCatalogo.builder()
                .tipo(ETipoCatalogo.TIPO_DOCUMENTO).nombre("CC").build());
        ValorCatalogo tipoInmueble = catalogos.save(ValorCatalogo.builder()
                .tipo(ETipoCatalogo.TIPO_INMUEBLE).nombre("Apartamento").build());
        Inmueble apto = inmuebles.save(Inmueble.builder()
                .torreBloque("A").numero("101").piso(1).tipo(tipoInmueble).build());
        Persona persona = personas.save(Persona.builder()
                .tipoPersona(residente).inmueble(apto).tipoDocumento(doc).numeroDocumento("123")
                .nombres("Ana").apellidos("Perez").fechaInicioVinculo(LocalDate.now().minusDays(1))
                .autorizaTratamientoDatos(true).build());
        cuentas.save(CuentaUsuario.builder().correoAcceso("ana@ph.com")
                .hashContrasena(codificador.encode(CLAVE)).rol(ERolCuenta.PERSONA_VINCULADA).persona(persona).build());

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenDe("ana@ph.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personaId").value(persona.getId()))
                .andExpect(jsonPath("$.autoridades.length()").value(2))
                .andExpect(jsonPath("$.autoridades[0]").value("ROLE_PERSONA_VINCULADA"))
                .andExpect(jsonPath("$.autoridades[1]").value("ROLE_RESIDENTE"));
    }

    // ---------- utilidades ----------

    private CuentaUsuario crearCuenta(String correo, ERolCuenta rol, boolean activa) {
        CuentaUsuario cuenta = CuentaUsuario.builder().correoAcceso(correo)
                .hashContrasena(codificador.encode(CLAVE)).rol(rol).activa(activa).build();
        return cuentas.save(cuenta);
    }

    private ResultActions login(String correo, String contrasena) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}"));
    }

    private String tokenDe(String correo) throws Exception {
        String cuerpo = login(correo, CLAVE).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.accessToken");
    }

    /** Fabrica un JWT HS256 arbitrario para probar que el servidor rechaza tokens no confiables. */
    private String firmar(String secreto, String emisor, Instant expira, List<String> autoridades) {
        var clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var codificadorJwt = new NimbusJwtEncoder(new ImmutableSecret<>(clave));
        var claims = JwtClaimsSet.builder().issuer(emisor).subject("1")
                .issuedAt(expira.minusSeconds(900)).expiresAt(expira)
                .claim(JwtTokenService.CLAIM_AUTORIDADES, autoridades).build();
        return codificadorJwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
