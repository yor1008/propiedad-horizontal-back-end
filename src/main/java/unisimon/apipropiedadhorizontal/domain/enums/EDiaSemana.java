package unisimon.apipropiedadhorizontal.domain.enums;

import java.time.DayOfWeek;

public enum EDiaSemana {
    LUNES,
    MARTES,
    MIERCOLES,
    JUEVES,
    VIERNES,
    SABADO,
    DOMINGO;

    public static EDiaSemana desde(DayOfWeek dia) {
        return values()[dia.ordinal()];
    }
}
