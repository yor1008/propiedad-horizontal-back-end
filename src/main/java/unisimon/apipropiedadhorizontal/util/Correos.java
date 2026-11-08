package unisimon.apipropiedadhorizontal.util;

import java.util.Locale;

public final class Correos {

    private Correos() {
    }

    /** Los correos se guardan y comparan en minusculas y sin espacios. */
    public static String normalizar(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }
}
