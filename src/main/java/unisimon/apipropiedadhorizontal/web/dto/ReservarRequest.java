package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** {@code numeroAsistentes} = invitados que asistiran (incluye a quien reserva). Formato: fecha yyyy-MM-dd, hora HH:mm. */
public record ReservarRequest(
        @NotNull Long zonaId,
        @NotNull LocalDate fecha,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @Min(1) int numeroAsistentes,
        @Size(max = 255) String motivo) {
}
