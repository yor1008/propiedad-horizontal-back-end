package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;

public record CrearCuentaRequest(
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank @Size(min = 8, max = 72) String contrasena,
        @NotNull ERolCuenta rol,
        Long personaId) {
}
