package unisimon.apipropiedadhorizontal.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import unisimon.apipropiedadhorizontal.domain.model.TipoPersona;
import unisimon.apipropiedadhorizontal.repository.TipoPersonaRepository;

/**
 * Siembra los tipos de persona del diagrama: Propietario y Residente (y la combinacion de ambos, porque una persona
 * puede ser las dos cosas). "Persona vinculada" no es un tipo: es el actor general que los agrupa.
 * Solo crea los que faltan por nombre.
 */
@Component
public class TiposPersonaIniciales implements ApplicationRunner {

    private final TipoPersonaRepository tipos;

    public TiposPersonaIniciales(TipoPersonaRepository tipos) {
        this.tipos = tipos;
    }

    @Override
    public void run(ApplicationArguments args) {
        sembrar("Propietario", "Dueno del inmueble que no lo habita", true, false);
        sembrar("Residente", "Habita el inmueble sin ser propietario", false, true);
        sembrar("Propietario residente", "Dueno que ademas habita el inmueble", true, true);
    }

    private void sembrar(String nombre, String descripcion, boolean propietario, boolean residente) {
        if (!tipos.existsByNombreIgnoreCase(nombre)) {
            tipos.save(TipoPersona.builder().nombre(nombre).descripcion(descripcion)
                    .esPropietario(propietario).esResidente(residente).build());
        }
    }
}
