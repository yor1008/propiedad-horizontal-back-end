package unisimon.apipropiedadhorizontal.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** {@code horarios}: franjas reservables ese dia de la semana; {@code ocupadas}: reservas vigentes ese dia. */
public record DisponibilidadResponse(LocalDate fecha, List<HorarioDto> horarios, List<FranjaOcupada> ocupadas) {

    public record FranjaOcupada(LocalTime horaInicio, LocalTime horaFin, String estado) {
    }
}
