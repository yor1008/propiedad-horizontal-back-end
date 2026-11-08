package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MotivoRequest(@NotBlank @Size(max = 255) String motivo) {
}
