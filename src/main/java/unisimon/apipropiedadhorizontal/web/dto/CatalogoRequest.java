package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;

public record CatalogoRequest(
        @NotNull ETipoCatalogo tipo,
        @NotBlank @Size(max = 100) String nombre) {
}
