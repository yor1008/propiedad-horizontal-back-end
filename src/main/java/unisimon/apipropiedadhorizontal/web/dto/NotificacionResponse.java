package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.model.Notificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(Long id, ETipoEvento evento, String mensaje, LocalDateTime fechaHora,
                                   boolean leida) {

    public static NotificacionResponse de(Notificacion n) {
        return new NotificacionResponse(n.getId(), n.getEvento(), n.getMensaje(), n.getFechaHora(), n.isLeida());
    }
}
