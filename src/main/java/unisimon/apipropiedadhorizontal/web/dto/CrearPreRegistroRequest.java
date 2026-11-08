package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoVigencia;

import java.time.LocalDate;

/**
 * {@code FECHA_ESPECIFICA} exige {@code fechaPrevista} (hoy o futura) y sirve para un solo ingreso; {@code PERMANENTE}
 * no lleva fecha y sirve hasta que se cancele. Vehiculo opcional: {@code tipoVehiculoId} de TIPO_VEHICULO.
 */
public record CrearPreRegistroRequest(
        @NotBlank @Size(max = 200) String nombreCompleto,
        @NotBlank @Size(max = 30) String numeroDocumento,
        @NotNull ETipoVigencia tipoVigencia,
        LocalDate fechaPrevista,
        @Size(max = 255) String motivo,
        @Size(max = 15) String placaVehiculo,
        Long tipoVehiculoId) {
}
