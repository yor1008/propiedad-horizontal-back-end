package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** La placa, la persona y el inmueble no cambian: se da de baja y se registra uno nuevo. */
public record ActualizarVehiculoRequest(
        @NotNull Long tipoId,
        @Size(max = 60) String marca,
        @Size(max = 40) String color) {
}
