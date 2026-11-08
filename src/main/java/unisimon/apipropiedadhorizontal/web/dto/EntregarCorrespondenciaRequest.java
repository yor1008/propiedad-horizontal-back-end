package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Quien recibe fisicamente el paquete. */
public record EntregarCorrespondenciaRequest(
        @NotBlank @Size(max = 150) String nombreRecibe,
        @NotBlank @Size(max = 30) String documentoRecibe) {
}
