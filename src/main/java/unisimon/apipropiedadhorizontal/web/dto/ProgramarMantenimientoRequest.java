package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * {@code zonaComunId} es opcional: si viene, la zona queda bloqueada para reservas durante el mantenimiento.
 * {@code fechaHoraFin} opcional (periodo abierto). Formato de fecha y hora: yyyy-MM-ddTHH:mm.
 */
public record ProgramarMantenimientoRequest(
        Long zonaComunId,
        @NotNull Long tipoId,
        @NotBlank @Size(max = 150) String elementoArea,
        @Size(max = 500) String descripcion,
        @NotNull LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        @Size(max = 150) String responsable) {
}
