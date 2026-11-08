package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearVehiculoRequest(
        @NotNull Long personaId,
        @NotNull Long inmuebleId,
        @NotBlank @Size(max = 15) @Pattern(regexp = "[A-Za-z0-9-]+", message = "solo letras, numeros y guion") String placa,
        @NotNull Long tipoId,
        @Size(max = 60) String marca,
        @Size(max = 40) String color) {
}
