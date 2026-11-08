package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Component;
import unisimon.apipropiedadhorizontal.domain.model.Inmueble;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.service.OcupacionInmuebles;

@Component
public class OcupacionInmueblesImpl implements OcupacionInmuebles {

    private final PersonaRepository personas;

    public OcupacionInmueblesImpl(PersonaRepository personas) {
        this.personas = personas;
    }

    @Override
    public void recalcular(Inmueble inmueble) {
        boolean ocupado = personas.findByInmuebleId(inmueble.getId()).stream()
                .filter(Persona::vinculoEstaVigente)
                .anyMatch(p -> p.getTipoPersona().isEsResidente());
        inmueble.marcarOcupado(ocupado);
    }
}
