package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionNotificacion;
import unisimon.apipropiedadhorizontal.domain.model.Notificacion;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.repository.ConfiguracionNotificacionRepository;
import unisimon.apipropiedadhorizontal.repository.NotificacionRepository;
import unisimon.apipropiedadhorizontal.service.EnviadorCorreo;
import unisimon.apipropiedadhorizontal.service.Notificador;

import java.util.Collection;
import java.util.List;

@Component
public class NotificadorImpl implements Notificador {

    private static final int MAXIMO_MENSAJE = 500;
    private static final String ASUNTO = "Notificación de su conjunto residencial";

    private final ConfiguracionNotificacionRepository configuraciones;
    private final NotificacionRepository notificaciones;
    private final EnviadorCorreo correo;

    public NotificadorImpl(ConfiguracionNotificacionRepository configuraciones, NotificacionRepository notificaciones,
                           EnviadorCorreo correo) {
        this.configuraciones = configuraciones;
        this.notificaciones = notificaciones;
        this.correo = correo;
    }

    @Override
    @Transactional
    public void notificar(ETipoEvento evento, Collection<Persona> destinatarios, String mensaje) {
        List<Persona> unicos = destinatarios.stream().filter(p -> p != null).distinct().toList();
        if (unicos.isEmpty()) {
            return;
        }
        ConfiguracionNotificacion configuracion = configuraciones.findByEvento(evento)
                .orElseGet(() -> ConfiguracionNotificacion.builder().evento(evento).build());
        String texto = mensaje.length() > MAXIMO_MENSAJE ? mensaje.substring(0, MAXIMO_MENSAJE - 1) + "…" : mensaje;
        List<Notificacion> creadas = notificaciones.saveAll(configuracion.generar(unicos, texto));
        // Se leen ahora (dentro de la transaccion) los correos: despues del commit la sesion ya no esta.
        List<String> direcciones = creadas.stream()
                .filter(Notificacion::isEnviadaPorCorreo)
                .map(n -> n.getPersona().getCorreo())
                .toList();
        enviarTrasConfirmar(direcciones, texto);
    }

    /** Si la transaccion se revierte no debe salir ningun correo: se envia solo despues del commit. */
    private void enviarTrasConfirmar(List<String> direcciones, String texto) {
        if (direcciones.isEmpty()) {
            return;
        }
        Runnable envio = () -> direcciones.forEach(d -> correo.enviar(d, ASUNTO, texto));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }
}
