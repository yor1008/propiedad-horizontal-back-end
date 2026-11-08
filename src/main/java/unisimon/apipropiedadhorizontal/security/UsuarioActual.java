package unisimon.apipropiedadhorizontal.security;

import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;

import java.util.List;
import java.util.Optional;

/**
 * Identidad de quien hace la peticion, derivada unicamente del JWT validado.
 * Los servicios dependen de esta abstraccion y no de SecurityContextHolder (DIP).
 */
public interface UsuarioActual {

    Long cuentaId();

    String correo();

    ERolCuenta rol();

    Optional<Long> personaId();

    List<String> autoridades();
}
