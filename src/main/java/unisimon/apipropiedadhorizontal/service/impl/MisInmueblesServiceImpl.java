package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.service.MisInmueblesService;
import unisimon.apipropiedadhorizontal.service.PersonaAutenticada;
import unisimon.apipropiedadhorizontal.web.dto.MiInmuebleResponse;
import unisimon.apipropiedadhorizontal.web.dto.ParqueaderoResponse;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MisInmueblesServiceImpl implements MisInmueblesService {

    private final PersonaAutenticada personaAutenticada;

    public MisInmueblesServiceImpl(PersonaAutenticada personaAutenticada) {
        this.personaAutenticada = personaAutenticada;
    }

    @Override
    public List<MiInmuebleResponse> consultar() {
        Persona persona = personaAutenticada.entidad();
        if (!persona.vinculoEstaVigente() || !persona.getTipoPersona().isEsPropietario()) {
            return List.of();
        }
        var inmueble = persona.getInmueble();
        List<ParqueaderoResponse> parqueaderos = inmueble.obtenerParqueaderos().stream()
                .map(ParqueaderoResponse::de).toList();
        return List.of(MiInmuebleResponse.de(inmueble, parqueaderos));
    }
}
