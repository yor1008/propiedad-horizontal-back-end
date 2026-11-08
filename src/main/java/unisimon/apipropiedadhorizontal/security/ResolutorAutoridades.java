package unisimon.apipropiedadhorizontal.security;

import org.springframework.stereotype.Component;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.TipoPersona;

import java.util.ArrayList;
import java.util.List;

/**
 * Traduce una cuenta a sus autoridades. Unica clase que conoce la regla
 * "Propietario/Residente son especializaciones de Persona vinculada con vinculo vigente".
 * Las autoridades se calculan al emitir el token (la app es stateless): un cambio posterior
 * en el vinculo se refleja en el siguiente login.
 */
@Component
public class ResolutorAutoridades {

    /** Requiere sesion JPA abierta (persona y tipo de persona son lazy). */
    public List<String> resolver(CuentaUsuario cuenta) {
        List<String> autoridades = new ArrayList<>();
        autoridades.add(Roles.autoridad(cuenta.getRol()));

        Persona persona = cuenta.getPersona();
        if (cuenta.getRol() == ERolCuenta.PERSONA_VINCULADA && persona != null && persona.vinculoEstaVigente()) {
            TipoPersona tipo = persona.getTipoPersona();
            if (tipo.isEsPropietario()) {
                autoridades.add(Roles.autoridad(Roles.PROPIETARIO));
            }
            if (tipo.isEsResidente()) {
                autoridades.add(Roles.autoridad(Roles.RESIDENTE));
            }
        }
        return List.copyOf(autoridades);
    }
}
