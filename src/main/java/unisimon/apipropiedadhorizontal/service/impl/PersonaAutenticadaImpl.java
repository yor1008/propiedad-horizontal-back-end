package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.security.UsuarioActual;
import unisimon.apipropiedadhorizontal.service.PersonaAutenticada;

@Component
public class PersonaAutenticadaImpl implements PersonaAutenticada {

    private final UsuarioActual usuarioActual;
    private final PersonaRepository personas;

    public PersonaAutenticadaImpl(UsuarioActual usuarioActual, PersonaRepository personas) {
        this.usuarioActual = usuarioActual;
        this.personas = personas;
    }

    @Override
    public Long id() {
        return usuarioActual.personaId()
                .orElseThrow(() -> new ReglaNegocioException("Su cuenta no tiene una persona asociada"));
    }

    @Override
    @Transactional(readOnly = true)
    public Persona entidad() {
        Long id = id();
        return personas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Persona", id));
    }
}
