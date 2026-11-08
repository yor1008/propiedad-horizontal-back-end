package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearPersonaRequest(
        @NotNull Long tipoPersonaId,
        @NotNull Long tipoDocumentoId,
        @NotBlank @Size(max = 30) String numeroDocumento,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @Size(max = 30) String telefono,
        @Email @Size(max = 150) String correo,
        /** Obligatorio si hay correo: el codigo de 6 digitos enviado con POST /api/personas/verificar-correo. */
        @Size(max = 10) String codigoVerificacion,
        boolean autorizaTratamientoDatos) {
}
