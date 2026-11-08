package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotNull;

/** Ambos campos son obligatorios (el PUT reemplaza la configuracion del evento). */
public record ConfiguracionNotificacionRequest(@NotNull Boolean activa, @NotNull Boolean enviarCorreo) {
}
