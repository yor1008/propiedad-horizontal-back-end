package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.Size;

public record FinalizarMantenimientoRequest(@Size(max = 500) String observaciones) {
}
