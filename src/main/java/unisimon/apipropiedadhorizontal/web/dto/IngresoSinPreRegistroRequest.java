package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import unisimon.apipropiedadhorizontal.domain.enums.EResultadoContacto;

/**
 * El vigilante contacta a un residente del inmueble visitado y registra su respuesta. {@code APRUEBA} deja entrar
 * al visitante; {@code NO_APRUEBA} o {@code NO_CONTESTA} niegan el ingreso (queda registrado en el historial).
 * {@code motivoNegacion} es opcional: si falta se usa un texto segun la respuesta.
 */
public record IngresoSinPreRegistroRequest(
        @NotBlank @Size(max = 200) String nombreCompleto,
        @NotBlank @Size(max = 30) String numeroDocumento,
        @NotNull Long inmuebleId,
        @NotNull Long residenteContactadoId,
        @NotNull EResultadoContacto respuestaResidente,
        @Size(max = 255) String motivoNegacion,
        Long tipoVehiculoId) {
}
