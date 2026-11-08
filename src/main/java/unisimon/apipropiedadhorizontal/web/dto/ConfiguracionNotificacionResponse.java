package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionNotificacion;

public record ConfiguracionNotificacionResponse(ETipoEvento evento, boolean activa, boolean enviarCorreo) {

    public static ConfiguracionNotificacionResponse de(ConfiguracionNotificacion c) {
        return new ConfiguracionNotificacionResponse(c.getEvento(), c.isActiva(), c.isEnviarCorreo());
    }
}
