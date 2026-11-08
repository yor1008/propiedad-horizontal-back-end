package unisimon.apipropiedadhorizontal.security;

import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;

/**
 * Nombres de autoridad. En el JWT viajan con prefijo {@code ROLE_} para que {@code hasRole(...)} funcione.
 * PROPIETARIO y RESIDENTE son especializaciones de PERSONA_VINCULADA (segun {@code TipoPersona}).
 */
public final class Roles {

    public static final String PREFIJO = "ROLE_";
    public static final String PROPIETARIO = "PROPIETARIO";
    public static final String RESIDENTE = "RESIDENTE";

    private Roles() {
    }

    public static String autoridad(ERolCuenta rol) {
        return PREFIJO + rol.name();
    }

    public static String autoridad(String nombreRol) {
        return PREFIJO + nombreRol;
    }
}
