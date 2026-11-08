package unisimon.apipropiedadhorizontal.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Fecha y hora locales del sistema a partir del {@link Clock} inyectado (que esta en UTC). Las entidades y el front
 * usan hora local (p. ej. {@code Persona.vinculoEstaVigente}), asi que las comparaciones deben hacerse con esta.
 */
public final class RelojLocal {

    private RelojLocal() {
    }

    public static LocalDate hoy(Clock reloj) {
        return LocalDate.now(reloj.withZone(ZoneId.systemDefault()));
    }

    public static LocalDateTime ahora(Clock reloj) {
        return LocalDateTime.now(reloj.withZone(ZoneId.systemDefault()));
    }
}
