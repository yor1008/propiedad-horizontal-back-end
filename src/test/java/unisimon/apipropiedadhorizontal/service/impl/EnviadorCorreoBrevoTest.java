package unisimon.apipropiedadhorizontal.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EnviadorCorreoBrevoTest {

    private static final String URL = "https://api.brevo.test/v3/smtp/email";

    @Test
    void enviaElCorreoALaApiConLaClaveYElRemitente() {
        RestClient.Builder constructor = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(constructor).build();
        servidor.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "clave-de-prueba"))
                .andExpect(jsonPath("$.sender.email").value("admin@ph.com"))
                .andExpect(jsonPath("$.to[0].email").value("ana@ph.com"))
                .andExpect(jsonPath("$.subject").value("Hola"))
                .andExpect(jsonPath("$.textContent").value("Cuerpo"))
                .andRespond(withSuccess("{\"messageId\":\"1\"}", MediaType.APPLICATION_JSON));

        new EnviadorCorreoBrevo(constructor.build(), URL, "clave-de-prueba", "admin@ph.com")
                .enviar("ana@ph.com", "Hola", "Cuerpo");

        servidor.verify();
    }

    @Test
    void unRechazoDeBrevoNoLanzaExcepcion() {
        RestClient.Builder constructor = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(constructor).build();
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"Key not found\"}"));

        assertDoesNotThrow(() -> new EnviadorCorreoBrevo(constructor.build(), URL, "mala", "admin@ph.com")
                .enviar("ana@ph.com", "Hola", "Cuerpo"));
        servidor.verify();
    }

    @Test
    void sinRemitenteNoLlamaALaApi() {
        RestClient.Builder constructor = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(constructor).build();

        new EnviadorCorreoBrevo(constructor.build(), URL, "clave", "").enviar("ana@ph.com", "Hola", "Cuerpo");

        servidor.verify(); // sin expectativas: cualquier llamada habria fallado
    }
}
