package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.Min;

/** {@code maxVehiculosPorInmueble}: 0 = sin limite. {@code plazoRespuestaPqrsDias}: dias habiles (lunes a viernes). */
public record ConfiguracionGeneralRequest(
        @Min(0) int maxVehiculosPorInmueble,
        @Min(1) int plazoRespuestaPqrsDias) {
}
