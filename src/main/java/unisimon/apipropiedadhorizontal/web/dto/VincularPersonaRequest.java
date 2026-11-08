package unisimon.apipropiedadhorizontal.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Vincula una persona a un inmueble; {@code fechaInicio} por defecto es hoy. */
public record VincularPersonaRequest(@NotNull Long personaId, @NotNull Long tipoPersonaId, LocalDate fechaInicio) {
}
