package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.Size;

/** Condiciones de uso opcionales que el administrador comunica al aprobar. */
public record AprobarReservaRequest(@Size(max = 500) String condiciones) {
}
