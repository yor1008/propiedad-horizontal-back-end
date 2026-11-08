package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code personaId} opcional: si falta, la correspondencia va dirigida al inmueble. */
public record RegistrarCorrespondenciaRequest(
        @NotNull Long inmuebleId,
        Long personaId,
        @NotNull Long tipoId,
        @Size(max = 150) String remitente,
        @Size(max = 500) String descripcion) {
}
