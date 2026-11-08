package unisimon.apipropiedadhorizontal.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import unisimon.apipropiedadhorizontal.service.EnviadorCorreo;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Envia por la API HTTP de Brevo (puerto 443), para hosts que bloquean el SMTP saliente (p. ej. Render gratis).
 * Se activa solo si {@code BREVO_API_KEY} tiene valor; si no, usa {@link EnviadorCorreoSmtp}. El remitente
 * ({@code MAIL_REMITENTE} o, si esta vacio, {@code MAIL_USERNAME}) debe estar verificado en Brevo.
 * Mejor esfuerzo y en segundo plano; nunca registra el cuerpo del correo (puede llevar enlaces o codigos).
 */
@Slf4j
@Component
@ConditionalOnExpression("!'${app.mail.brevo.api-key:}'.isEmpty()")
public class EnviadorCorreoBrevo implements EnviadorCorreo {

    private final RestClient cliente;
    private final String url;
    private final String claveApi;
    private final String remitente;

    @Autowired
    public EnviadorCorreoBrevo(@Value("${app.mail.brevo.url}") String url,
                               @Value("${app.mail.brevo.api-key}") String claveApi,
                               @Value("${app.mail.remitente:}") String remitente,
                               @Value("${spring.mail.username:}") String usuario) {
        this(RestClient.builder().requestFactory(factoria()).build(), url, claveApi,
                remitente.isBlank() ? usuario : remitente);
    }

    EnviadorCorreoBrevo(RestClient cliente, String url, String claveApi, String remitente) {
        this.cliente = cliente;
        this.url = url;
        this.claveApi = claveApi;
        this.remitente = remitente;
    }

    @Override
    @Async
    public void enviar(String destinatario, String asunto, String texto) {
        if (remitente.isBlank()) {
            log.warn("Correo no enviado: falta el remitente (defina MAIL_REMITENTE, verificado en Brevo). Asunto: {}", asunto);
            return;
        }
        Map<String, Object> cuerpo = Map.of(
                "sender", Map.of("name", "Propiedad Horizontal", "email", remitente),
                "to", List.of(Map.of("email", destinatario)),
                "subject", asunto,
                "textContent", texto);
        try {
            cliente.post().uri(url).header("api-key", claveApi).body(cuerpo).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            // La respuesta de Brevo explica el rechazo (remitente sin verificar, clave invalida...); no incluye nuestro correo.
            log.error("Brevo rechazo el correo '{}': HTTP {} {}", asunto, e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el correo '{}' por Brevo: {}", asunto, e.getClass().getSimpleName());
        }
    }

    private static JdkClientHttpRequestFactory factoria() {
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory();
        f.setReadTimeout(Duration.ofSeconds(10));
        return f;
    }
}
