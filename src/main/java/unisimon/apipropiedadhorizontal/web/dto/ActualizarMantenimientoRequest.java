package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Solo se puede modificar un mantenimiento aun programado; zona, tipo y elemento no cambian. */
public record ActualizarMantenimientoRequest(
        @Size(max = 500) String descripcion,
        @NotNull LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        @Size(max = 150) String responsable) {
}
