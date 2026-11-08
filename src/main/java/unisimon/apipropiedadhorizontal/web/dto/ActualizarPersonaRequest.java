package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos editables de una persona (por el administrador o por ella misma en su perfil). El documento no cambia. */
public record ActualizarPersonaRequest(
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @Size(max = 30) String telefono,
        @Email @Size(max = 150) String correo) {
}
