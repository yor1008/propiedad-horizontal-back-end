package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import unisimon.apipropiedadhorizontal.domain.enums.EPeriodoLimite;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoUsoZona;

import java.util.List;

/**
 * Crear y actualizar usan el mismo cuerpo. {@code horarios} exige al menos uno (el UML dice 1..*).
 * 0 en una regla numerica = sin restriccion. {@code periodoLimite} es obligatorio si {@code maxReservasPorInmueble > 0}.
 */
public record ZonaComunRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 500) String descripcion,
        @Min(1) int capacidadMaxima,
        @NotNull ETipoUsoZona tipoUso,
        boolean requiereAprobacion,
        @Min(0) int duracionMaximaMinutos,
        @Min(0) int anticipacionMinimaHoras,
        @Min(0) int anticipacionMaximaDias,
        @Min(0) int limiteCancelacionHoras,
        @Min(0) int maxReservasPorInmueble,
        EPeriodoLimite periodoLimite,
        @NotEmpty @Valid List<HorarioDto> horarios) {
}
