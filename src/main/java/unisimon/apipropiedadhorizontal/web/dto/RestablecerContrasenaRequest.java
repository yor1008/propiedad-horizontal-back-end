package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code token} es el que viene en el enlace del correo; el maximo de 72 es el limite de bytes de BCrypt. */
public record RestablecerContrasenaRequest(
        @NotBlank @Size(max = 2000) String token,
        @NotBlank @Size(min = 8, max = 72) String nueva) {
}
