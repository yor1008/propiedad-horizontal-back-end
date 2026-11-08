package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.TipoPersona;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.repository.TipoPersonaRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.service.HistorialPersona;
import unisimon.apipropiedadhorizontal.service.OcupacionInmuebles;
import unisimon.apipropiedadhorizontal.service.PersonaService;
import unisimon.apipropiedadhorizontal.util.Correos;
import unisimon.apipropiedadhorizontal.web.dto.ActualizarPersonaRequest;
import unisimon.apipropiedadhorizontal.web.dto.CrearPersonaRequest;
import unisimon.apipropiedadhorizontal.web.dto.PersonaResponse;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PersonaServiceImpl implements PersonaService {

    private final PersonaRepository personas;
    private final TipoPersonaRepository tiposPersona;
    private final ValorCatalogoRepository catalogos;
    private final Clock reloj;
    private final HistorialPersona historial;
    private final OcupacionInmuebles ocupacion;

    public PersonaServiceImpl(PersonaRepository personas, TipoPersonaRepository tiposPersona,
                              ValorCatalogoRepository catalogos, Clock reloj,
                              HistorialPersona historial, OcupacionInmuebles ocupacion) {
        this.personas = personas;
        this.tiposPersona = tiposPersona;
        this.catalogos = catalogos;
        this.reloj = reloj;
        this.historial = historial;
        this.ocupacion = ocupacion;
    }

    @Override
    public PersonaResponse crear(CrearPersonaRequest s) {
        TipoPersona tipo = tiposPersona.findById(s.tipoPersonaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de persona", s.tipoPersonaId()));
        if (!tipo.isActivo()) {
            throw new ReglaNegocioException("El tipo de persona esta inactivo");
        }
        ValorCatalogo tipoDocumento = catalogos.findById(s.tipoDocumentoId())
                .filter(v -> v.getTipo() == ETipoCatalogo.TIPO_DOCUMENTO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de documento", s.tipoDocumentoId()));
        if (!tipoDocumento.isActivo()) {
            throw new ReglaNegocioException("El tipo de documento esta inactivo");
        }
        String numero = s.numeroDocumento().trim();
        if (personas.findByTipoDocumentoIdAndNumeroDocumento(tipoDocumento.getId(), numero).isPresent()) {
            throw new ConflictoException("Ya existe una persona con ese documento");
        }
        Persona persona = Persona.builder()
                .tipoPersona(tipo)
                .tipoDocumento(tipoDocumento)
                .numeroDocumento(numero)
                .nombres(s.nombres().trim())
                .apellidos(s.apellidos().trim())
                .telefono(s.telefono())
                .correo(s.correo() == null || s.correo().isBlank() ? null : Correos.normalizar(s.correo()))
                .autorizaTratamientoDatos(s.autorizaTratamientoDatos())
                .fechaAutorizacionDatos(s.autorizaTratamientoDatos() ? LocalDate.now(reloj) : null)
                .build();
        return PersonaResponse.de(personas.save(persona));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaResponse> listar() {
        return personas.findAll().stream().map(PersonaResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaResponse obtener(Long id) {
        return PersonaResponse.de(personas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona", id)));
    }

    @Override
    public PersonaResponse actualizar(Long id, ActualizarPersonaRequest s) {
        Persona persona = buscar(id);
        persona.actualizarDatos(s.nombres().trim(), s.apellidos().trim(), s.telefono(),
                s.correo() == null || s.correo().isBlank() ? null : Correos.normalizar(s.correo()));
        return PersonaResponse.de(persona);
    }

    @Override
    public PersonaResponse desactivar(Long id, String motivo) {
        Persona persona = buscar(id);
        persona.desactivar(motivo.trim());
        recalcularOcupacion(persona);
        return PersonaResponse.de(persona);
    }

    @Override
    public PersonaResponse reactivar(Long id) {
        Persona persona = buscar(id);
        persona.reactivar();
        recalcularOcupacion(persona);
        return PersonaResponse.de(persona);
    }

    @Override
    public void eliminar(Long id) {
        Persona persona = buscar(id);
        if (historial.tieneHistorial(persona)) {
            throw new ConflictoException("La persona tiene historia asociada (vinculo, cuenta, vehiculos, PQRS, "
                    + "reservas...) y no se puede borrar; desactivela en su lugar");
        }
        personas.delete(persona);
    }

    /** Una persona inactiva deja de tener vinculo vigente, asi que cambia la ocupacion de su inmueble. */
    private void recalcularOcupacion(Persona persona) {
        if (persona.getInmueble() != null) {
            personas.flush();
            ocupacion.recalcular(persona.getInmueble());
        }
    }

    private Persona buscar(Long id) {
        return personas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Persona", id));
    }
}
