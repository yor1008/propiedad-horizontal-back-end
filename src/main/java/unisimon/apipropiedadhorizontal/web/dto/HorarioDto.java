package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotNull;
import unisimon.apipropiedadhorizontal.domain.enums.EDiaSemana;
import unisimon.apipropiedadhorizontal.domain.model.HorarioDisponibilidad;

import java.time.LocalTime;

/** Franja horaria de una zona; sirve de entrada y de salida. Formato de hora: HH:mm. */
public record HorarioDto(@NotNull EDiaSemana diaSemana, @NotNull LocalTime horaInicio, @NotNull LocalTime horaFin) {

    public static HorarioDto de(HorarioDisponibilidad h) {
        return new HorarioDto(h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin());
    }
}
