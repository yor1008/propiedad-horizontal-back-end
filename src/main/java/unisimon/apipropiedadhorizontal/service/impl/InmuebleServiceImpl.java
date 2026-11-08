package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ECategoriaInmueble;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.Inmueble;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.TipoPersona;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.InmuebleRepository;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.repository.TipoPersonaRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.service.HistorialInmueble;
import unisimon.apipropiedadhorizontal.service.InmuebleService;
import unisimon.apipropiedadhorizontal.service.OcupacionInmuebles;
import unisimon.apipropiedadhorizontal.web.dto.CrearInmuebleRequest;
import unisimon.apipropiedadhorizontal.web.dto.MotivoRequest;
import unisimon.apipropiedadhorizontal.web.dto.InmuebleResponse;
import unisimon.apipropiedadhorizontal.web.dto.PersonaResponse;
import unisimon.apipropiedadhorizontal.web.dto.VincularPersonaRequest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class InmuebleServiceImpl implements InmuebleService {

    private final InmuebleRepository inmuebles;
    private final PersonaRepository personas;
    private final TipoPersonaRepository tiposPersona;
    private final ValorCatalogoRepository catalogos;
    private final Clock reloj;
    private final OcupacionInmuebles ocupacion;
    private final HistorialInmueble historial;

    public InmuebleServiceImpl(InmuebleRepository inmuebles, PersonaRepository personas,
                               TipoPersonaRepository tiposPersona, ValorCatalogoRepository catalogos, Clock reloj,
                               OcupacionInmuebles ocupacion, HistorialInmueble historial) {
        this.inmuebles = inmuebles;
        this.personas = personas;
        this.tiposPersona = tiposPersona;
        this.catalogos = catalogos;
        this.reloj = reloj;
        this.ocupacion = ocupacion;
        this.historial = historial;
    }

    @Override
    public InmuebleResponse crear(CrearInmuebleRequest s) {
        String torre = s.torreBloque().trim();
        String numero = s.numero().trim();
        if (inmuebles.existsByTorreBloqueAndNumero(torre, numero)) {
            throw new ConflictoException("Ya existe el inmueble " + torre + "-" + numero);
        }
        Inmueble inmueble = inmuebles.save(Inmueble.builder()
                .torreBloque(torre).numero(numero).piso(s.piso()).tipo(tipoInmueble(s.tipoId())).build());
        return InmuebleResponse.de(inmueble, null);
    }

    @Override
    public InmuebleResponse actualizar(Long id, CrearInmuebleRequest s) {
        Inmueble inmueble = buscarUnidad(id);
        String torre = s.torreBloque().trim();
        String numero = s.numero().trim();
        if (inmuebles.existsByTorreBloqueAndNumeroAndIdNot(torre, numero, id)) {
            throw new ConflictoException("Ya existe el inmueble " + torre + "-" + numero);
        }
        inmueble.modificarDatos(torre, numero, s.piso(), tipoInmueble(s.tipoId()));
        return aRespuesta(inmueble);
    }

    @Override
    public InmuebleResponse desactivar(Long id, String motivo) {
        Inmueble inmueble = buscarUnidad(id);
        boolean conVinculosVigentes = personas.findByInmuebleId(id).stream().anyMatch(Persona::vinculoEstaVigente);
        if (conVinculosVigentes) {
            throw new ReglaNegocioException("El inmueble tiene personas con vinculo vigente; finalice los vinculos primero");
        }
        inmueble.desactivar(motivo.trim());
        return aRespuesta(inmueble);
    }

    @Override
    public InmuebleResponse reactivar(Long id) {
        Inmueble inmueble = buscarUnidad(id);
        inmueble.reactivar();
        return aRespuesta(inmueble);
    }

    @Override
    public void eliminar(Long id) {
        Inmueble inmueble = buscarUnidad(id);
        if (historial.tieneHistorial(inmueble)) {
            throw new ConflictoException("El inmueble tiene historia asociada (personas, vehiculos, PQRS, reservas, "
                    + "visitas...) y no se puede borrar; desactivelo en su lugar");
        }
        inmuebles.delete(inmueble);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InmuebleResponse> listar() {
        return inmuebles.findByCategoria(ECategoriaInmueble.UNIDAD_PRIVADA).stream().map(this::aRespuesta).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InmuebleResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaResponse> personasDe(Long inmuebleId) {
        buscar(inmuebleId);
        return personas.findByInmuebleId(inmuebleId).stream()
                .filter(Persona::vinculoEstaVigente)
                .map(PersonaResponse::de)
                .toList();
    }

    @Override
    public PersonaResponse vincular(Long inmuebleId, VincularPersonaRequest s) {
        Inmueble inmueble = buscar(inmuebleId);
        if (!inmueble.isActivo()) {
            throw new ReglaNegocioException("El inmueble esta inactivo");
        }
        if (inmueble.getCategoria() != ECategoriaInmueble.UNIDAD_PRIVADA) {
            throw new ReglaNegocioException("Un parqueadero no se vincula a personas");
        }
        Persona persona = personas.findById(s.personaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona", s.personaId()));
        if (!persona.isActiva()) {
            throw new ReglaNegocioException("La persona esta inactiva");
        }
        if (persona.vinculoEstaVigente()) {
            throw new ConflictoException("La persona ya tiene un vinculo vigente; finalicelo antes de crear otro");
        }
        TipoPersona tipo = tiposPersona.findById(s.tipoPersonaId())
                .filter(TipoPersona::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de persona", s.tipoPersonaId()));
        LocalDate inicio = s.fechaInicio() == null ? hoy() : s.fechaInicio();
        persona.vincularInmueble(inmueble, tipo, inicio);
        personas.flush();
        ocupacion.recalcular(inmueble);
        return PersonaResponse.de(persona);
    }

    @Override
    public PersonaResponse finalizarVinculo(Long personaId, MotivoRequest s) {
        Persona persona = personas.findById(personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona", personaId));
        if (!persona.vinculoEstaVigente()) {
            throw new ReglaNegocioException("La persona no tiene un vinculo vigente");
        }
        Inmueble inmueble = persona.getInmueble();
        // Ayer: vinculoEstaVigente() acepta fin >= hoy, asi que "hoy" seguiria vigente.
        persona.finalizarVinculo(hoy().minusDays(1), s.motivo().trim());
        personas.flush();
        ocupacion.recalcular(inmueble);
        return PersonaResponse.de(persona);
    }

    /** Fecha local del sistema: debe coincidir con Persona.vinculoEstaVigente(), que usa LocalDate.now(). */
    private LocalDate hoy() {
        return LocalDate.now(reloj.withZone(ZoneId.systemDefault()));
    }

    private Inmueble buscar(Long id) {
        return inmuebles.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Inmueble", id));
    }

    /** Solo unidades privadas: los parqueaderos se gestionan en /api/parqueaderos. */
    private Inmueble buscarUnidad(Long id) {
        return inmuebles.findById(id)
                .filter(i -> i.getCategoria() == ECategoriaInmueble.UNIDAD_PRIVADA)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inmueble", id));
    }

    private ValorCatalogo tipoInmueble(Long id) {
        ValorCatalogo tipo = catalogos.findById(id)
                .filter(v -> v.getTipo() == ETipoCatalogo.TIPO_INMUEBLE)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de inmueble", id));
        if (!tipo.isActivo()) {
            throw new ReglaNegocioException("El tipo de inmueble esta inactivo");
        }
        return tipo;
    }

    private InmuebleResponse aRespuesta(Inmueble inmueble) {
        String propietario = personas.findByInmuebleId(inmueble.getId()).stream()
                .filter(p -> p.vinculoEstaVigente() && p.getTipoPersona().isEsPropietario())
                .map(p -> p.getNombres() + " " + p.getApellidos())
                .findFirst().orElse(null);
        return InmuebleResponse.de(inmueble, propietario);
    }
}
