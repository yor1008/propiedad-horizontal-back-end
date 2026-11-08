package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtenderPqrsRequest(@NotBlank @Size(max = 2000) String respuesta) {
}
