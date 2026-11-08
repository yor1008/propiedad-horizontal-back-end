package unisimon.apipropiedadhorizontal.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import unisimon.apipropiedadhorizontal.service.EnviadorCorreo;

/**
 * Envia por SMTP en segundo plano ({@code @Async}) para que un servidor lento no retrase la peticion. Nunca registra
 * el cuerpo del correo (puede contener el enlace de restablecimiento). Sin {@code MAIL_HOST} no envia y avisa en el log.
 */
@Slf4j
@Component
public class EnviadorCorreoSmtp implements EnviadorCorreo {

    private final ObjectProvider<JavaMailSender> emisor;
    private final String host;
    private final String usuario;
    private final String remitente;

    public EnviadorCorreoSmtp(ObjectProvider<JavaMailSender> emisor,
                              @Value("${spring.mail.host:}") String host,
                              @Value("${spring.mail.username:}") String usuario,
                              @Value("${app.mail.remitente:}") String remitente) {
        this.emisor = emisor;
        this.host = host;
        this.usuario = usuario;
        this.remitente = remitente;
    }

    @Override
    @Async
    public void enviar(String destinatario, String asunto, String texto) {
        JavaMailSender sender = emisor.getIfAvailable();
        if (sender == null || host.isBlank()) {
            log.warn("Correo no enviado: SMTP sin configurar (defina MAIL_HOST). Asunto: {}", asunto);
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            String desde = remitente.isBlank() ? usuario : remitente;
            if (!desde.isBlank()) {
                mensaje.setFrom(desde);
            }
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            sender.send(mensaje);
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el correo '{}': {}", asunto, e.getClass().getSimpleName());
        }
    }
}
