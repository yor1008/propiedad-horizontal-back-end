package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** El maximo de 72 es el limite de bytes de BCrypt. */
public record CambiarContrasenaRequest(
        @NotBlank @Size(max = 72) String actual,
        @NotBlank @Size(min = 8, max = 72) String nueva) {
}
