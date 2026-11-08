package unisimon.apipropiedadhorizontal.service.impl;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EnviadorCorreoGmailTest {

    private static final String TOKEN = "https://oauth.test/token";
    private static final String ENVIO = "https://gmail.test/send";

    /** Reloj que se puede adelantar, para probar el vencimiento del access token. */
    private static final class RelojAvanzable extends Clock {
        private Instant ahora = Instant.parse("2026-11-08T12:00:00Z");

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private static EnviadorCorreoGmail enviador(RestClient.Builder b, Clock reloj, String id, String secreto) {
        return new EnviadorCorreoGmail(b.build(), reloj, TOKEN, ENVIO, id, secreto, "refresh-123", "admin@ph.com");
    }

    @Test
    void cambiaElRefreshTokenYEnviaElCorreoCodificado() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(b).build();
        servidor.expect(requestTo(TOKEN)).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(allOf(containsString("grant_type=refresh_token"),
                        containsString("refresh_token=refresh-123"), containsString("client_id=id"),
                        containsString("client_secret=secreto"))))
                .andRespond(withSuccess("{\"access_token\":\"acceso-1\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(ENVIO)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer acceso-1"))
                .andExpect(request -> {
                    String cuerpo = ((MockClientHttpRequest) request).getBodyAsString();
                    String mime = new String(Base64.getUrlDecoder().decode(JsonPath.<String>read(cuerpo, "$.raw")),
                            StandardCharsets.UTF_8);
                    assertTrue(mime.contains("To: ana@ph.com"), mime);
                    assertTrue(mime.contains("From: admin@ph.com"), mime);
                    assertTrue(mime.contains("Subject: "), mime);
                    assertTrue(mime.contains("Su codigo es 123456"), mime);
                })
                .andRespond(withSuccess("{\"id\":\"1\"}", MediaType.APPLICATION_JSON));

        enviador(b, new RelojAvanzable(), "id", "secreto").enviar("ana@ph.com", "Código de verificación", "Su codigo es 123456");

        servidor.verify();
    }

    @Test
    void reutilizaElAccessTokenMientrasEsteVigenteYLoRenuevaAlVencer() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(b).build();
        servidor.expect(requestTo(TOKEN)).andRespond(withSuccess(
                "{\"access_token\":\"a1\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(ENVIO)).andExpect(header("Authorization", "Bearer a1"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        // Segundo envio a los 10 min: reutiliza a1 (no hay nueva llamada al token).
        servidor.expect(requestTo(ENVIO)).andExpect(header("Authorization", "Bearer a1"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        // Tercer envio pasada la hora: pide otro token.
        servidor.expect(requestTo(TOKEN)).andRespond(withSuccess(
                "{\"access_token\":\"a2\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(ENVIO)).andExpect(header("Authorization", "Bearer a2"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        RelojAvanzable reloj = new RelojAvanzable();
        EnviadorCorreoGmail enviador = enviador(b, reloj, "id", "secreto");
        enviador.enviar("a@ph.com", "Uno", "x");
        reloj.avanzar(Duration.ofMinutes(10));
        enviador.enviar("a@ph.com", "Dos", "x");
        reloj.avanzar(Duration.ofHours(2));
        enviador.enviar("a@ph.com", "Tres", "x");

        servidor.verify();
    }

    @Test
    void unRechazoDeGoogleNoLanzaExcepcion() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(b).build();
        servidor.expect(requestTo(TOKEN)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"invalid_grant\"}"));

        assertDoesNotThrow(() -> enviador(b, new RelojAvanzable(), "id", "secreto").enviar("ana@ph.com", "Hola", "x"));
        servidor.verify();
    }

    @Test
    void sinCredencialesDeClienteNoLlamaAGoogle() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(b).build();

        enviador(b, new RelojAvanzable(), "", "").enviar("ana@ph.com", "Hola", "x");

        servidor.verify(); // sin expectativas: cualquier llamada habria fallado
    }
}
