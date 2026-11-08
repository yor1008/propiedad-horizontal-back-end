package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.service.PerfilService;
import unisimon.apipropiedadhorizontal.service.PersonaAutenticada;
import unisimon.apipropiedadhorizontal.util.Correos;
import unisimon.apipropiedadhorizontal.web.dto.ActualizarPersonaRequest;
import unisimon.apipropiedadhorizontal.web.dto.PersonaResponse;

@Service
@Transactional
public class PerfilServiceImpl implements PerfilService {

    private final PersonaAutenticada personaAutenticada;

    public PerfilServiceImpl(PersonaAutenticada personaAutenticada) {
        this.personaAutenticada = personaAutenticada;
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaResponse obtener() {
        return PersonaResponse.de(personaAutenticada.entidad());
    }

    @Override
    public PersonaResponse actualizar(ActualizarPersonaRequest s) {
        Persona persona = personaAutenticada.entidad();
        persona.actualizarDatos(s.nombres().trim(), s.apellidos().trim(), s.telefono(),
                s.correo() == null || s.correo().isBlank() ? null : Correos.normalizar(s.correo()));
        return PersonaResponse.de(persona);
    }
}
