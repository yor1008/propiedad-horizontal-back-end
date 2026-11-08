package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Correo de acceso de la cuenta. No se valida el formato como email: la respuesta no debe delatar nada. */
public record OlvideContrasenaRequest(@NotBlank @Size(max = 150) String correo) {
}
