package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarParqueaderoVisitanteRequest(@NotNull Long parqueaderoId) {
}
