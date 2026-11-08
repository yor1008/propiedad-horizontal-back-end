package unisimon.apipropiedadhorizontal.service.impl;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import unisimon.apipropiedadhorizontal.service.EnviadorCorreo;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

/**
 * Envia desde una cuenta de Gmail con la Gmail API (OAuth2, HTTPS puerto 443), para hosts que bloquean el SMTP
 * saliente (p. ej. Render gratis). Se activa solo si {@code GMAIL_REFRESH_TOKEN} tiene valor; si no, usa SMTP.
 * <p>
 * El refresh token (scope {@code gmail.send}, obtenido una vez con la cuenta emisora) se cambia por un access token de
 * corta duracion que se guarda en memoria hasta que vence. Mejor esfuerzo y en segundo plano; nunca registra el
 * cuerpo del correo (puede llevar enlaces o codigos) ni los tokens.
 */
@Slf4j
@Component
@ConditionalOnExpression("!'${app.mail.gmail.refresh-token:}'.isEmpty()")
public class EnviadorCorreoGmail implements EnviadorCorreo {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() { };
    /** Margen para no usar un access token a punto de vencer. */
    private static final Duration MARGEN = Duration.ofSeconds(60);

    private final RestClient cliente;
    private final Clock reloj;
    private final String urlToken;
    private final String urlEnvio;
    private final String idCliente;
    private final String secretoCliente;
    private final String refreshToken;
    private final String remitente;

    private String accessToken;
    private Instant accessTokenVenceEn = Instant.MIN;

    @Autowired
    public EnviadorCorreoGmail(@Value("${app.mail.gmail.token-url}") String urlToken,
                               @Value("${app.mail.gmail.send-url}") String urlEnvio,
                               @Value("${app.mail.gmail.client-id:}") String idCliente,
                               @Value("${app.mail.gmail.client-secret:}") String secretoCliente,
                               @Value("${app.mail.gmail.refresh-token}") String refreshToken,
                               @Value("${app.mail.remitente:}") String remitente,
                               @Value("${spring.mail.username:}") String usuario,
                               Clock reloj) {
        this(RestClient.builder().requestFactory(factoria()).build(), reloj, urlToken, urlEnvio, idCliente,
                secretoCliente, refreshToken, remitente.isBlank() ? usuario : remitente);
    }

    EnviadorCorreoGmail(RestClient cliente, Clock reloj, String urlToken, String urlEnvio, String idCliente,
                        String secretoCliente, String refreshToken, String remitente) {
        this.cliente = cliente;
        this.reloj = reloj;
        this.urlToken = urlToken;
        this.urlEnvio = urlEnvio;
        this.idCliente = idCliente;
        this.secretoCliente = secretoCliente;
        this.refreshToken = refreshToken;
        this.remitente = remitente;
    }

    @Override
    @Async
    public void enviar(String destinatario, String asunto, String texto) {
        if (idCliente.isBlank() || secretoCliente.isBlank()) {
            log.warn("Correo no enviado: faltan GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET. Asunto: {}", asunto);
            return;
        }
        try {
            String raw = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(construirMensaje(destinatario, asunto, texto));
            cliente.post().uri(urlEnvio)
                    .header("Authorization", "Bearer " + obtenerAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("raw", raw))
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            // La respuesta de Google explica el rechazo (invalid_grant, scope insuficiente...); no incluye nuestro correo.
            log.error("Google rechazo el envio del correo '{}': HTTP {} {}", asunto, e.getStatusCode().value(),
                    e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("No se pudo enviar el correo '{}' por Gmail: {}", asunto, e.getClass().getSimpleName());
        }
    }

    /** Access token vigente; lo renueva con el refresh token cuando falta o esta por vencer. */
    private synchronized String obtenerAccessToken() {
        Instant ahora = reloj.instant();
        if (accessToken != null && ahora.isBefore(accessTokenVenceEn.minus(MARGEN))) {
            return accessToken;
        }
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("client_id", idCliente);
        formulario.add("client_secret", secretoCliente);
        formulario.add("refresh_token", refreshToken);
        formulario.add("grant_type", "refresh_token");
        Map<String, Object> respuesta = cliente.post().uri(urlToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario).retrieve().body(MAPA);
        if (respuesta == null || !(respuesta.get("access_token") instanceof String token)) {
            throw new IllegalStateException("Google no devolvio access_token");
        }
        long segundos = respuesta.get("expires_in") instanceof Number n ? n.longValue() : 3600;
        accessToken = token;
        accessTokenVenceEn = ahora.plusSeconds(segundos);
        return token;
    }

    private byte[] construirMensaje(String destinatario, String asunto, String texto) throws Exception {
        MimeMessage mensaje = new MimeMessage(Session.getInstance(new Properties()));
        if (!remitente.isBlank()) {
            mensaje.setFrom(new InternetAddress(remitente));
        }
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        mensaje.setSubject(asunto, "UTF-8");
        mensaje.setText(texto, "UTF-8");
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        mensaje.writeTo(salida);
        return salida.toByteArray();
    }

    private static JdkClientHttpRequestFactory factoria() {
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory();
        f.setReadTimeout(Duration.ofSeconds(10));
        return f;
    }
}
