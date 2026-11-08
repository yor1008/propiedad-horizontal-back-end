package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearInmuebleRequest(
        @NotBlank @Size(max = 30) String torreBloque,
        @NotBlank @Size(max = 30) String numero,
        @Min(0) int piso,
        @NotNull Long tipoId) {
}
