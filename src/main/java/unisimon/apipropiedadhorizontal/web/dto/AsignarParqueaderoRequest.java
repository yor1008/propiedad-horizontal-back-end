package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarParqueaderoRequest(@NotNull Long inmuebleId) {
}
