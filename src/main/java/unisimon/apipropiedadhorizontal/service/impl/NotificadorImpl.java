package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionNotificacion;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.repository.ConfiguracionNotificacionRepository;
import unisimon.apipropiedadhorizontal.repository.NotificacionRepository;
import unisimon.apipropiedadhorizontal.service.Notificador;

import java.util.Collection;
import java.util.List;

@Component
public class NotificadorImpl implements Notificador {

    private static final int MAXIMO_MENSAJE = 500;

    private final ConfiguracionNotificacionRepository configuraciones;
    private final NotificacionRepository notificaciones;

    public NotificadorImpl(ConfiguracionNotificacionRepository configuraciones, NotificacionRepository notificaciones) {
        this.configuraciones = configuraciones;
        this.notificaciones = notificaciones;
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
        notificaciones.saveAll(configuracion.generar(unicos, texto));
    }
}
