package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code adjunto}: referencia opcional (nombre o ruta) a un archivo; la API no almacena el archivo. */
public record RadicarPqrsRequest(
        @NotNull Long tipoId,
        @NotBlank @Size(max = 200) String asunto,
        @NotBlank @Size(max = 2000) String descripcion,
        @Size(max = 500) String adjunto) {
}
