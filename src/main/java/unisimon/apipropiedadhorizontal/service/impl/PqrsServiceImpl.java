package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.NombresEstado;
import unisimon.apipropiedadhorizontal.domain.enums.EProcesoEstado;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionGeneral;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.Pqrs;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.ConfiguracionGeneralRepository;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.repository.PqrsRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.security.UsuarioActual;
import unisimon.apipropiedadhorizontal.service.GestorEstados;
import unisimon.apipropiedadhorizontal.service.Notificador;
import unisimon.apipropiedadhorizontal.service.PqrsService;
import unisimon.apipropiedadhorizontal.web.dto.AtenderPqrsRequest;
import unisimon.apipropiedadhorizontal.web.dto.PqrsResponse;
import unisimon.apipropiedadhorizontal.web.dto.RadicarPqrsRequest;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PqrsServiceImpl implements PqrsService {

    private final PqrsRepository pqrs;
    private final PersonaRepository personas;
    private final ValorCatalogoRepository catalogos;
    private final ConfiguracionGeneralRepository configuracion;
    private final GestorEstados estados;
    private final UsuarioActual usuarioActual;
    private final Notificador notificador;

    public PqrsServiceImpl(PqrsRepository pqrs, PersonaRepository personas, ValorCatalogoRepository catalogos,
                           ConfiguracionGeneralRepository configuracion, GestorEstados estados,
                           UsuarioActual usuarioActual, Notificador notificador) {
        this.pqrs = pqrs;
        this.personas = personas;
        this.catalogos = catalogos;
        this.configuracion = configuracion;
        this.estados = estados;
        this.usuarioActual = usuarioActual;
        this.notificador = notificador;
    }

    @Override
    public PqrsResponse radicar(RadicarPqrsRequest s) {
        Persona persona = personaActual();
        if (!persona.vinculoEstaVigente()) {
            throw new ReglaNegocioException("Necesita un vinculo vigente con un inmueble para radicar una PQRS");
        }
        ValorCatalogo tipo = catalogos.findById(s.tipoId())
                .filter(v -> v.getTipo() == ETipoCatalogo.TIPO_PQRS)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de PQRS", s.tipoId()));
        if (!tipo.isActivo()) {
            throw new ReglaNegocioException("El tipo de PQRS esta inactivo");
        }
        String adjunto = s.adjunto() == null || s.adjunto().isBlank() ? null : s.adjunto().trim();
        Pqrs nueva = pqrs.save(Pqrs.builder()
                .persona(persona).inmueble(persona.getInmueble()).tipo(tipo)
                .asunto(s.asunto().trim()).descripcion(s.descripcion().trim()).adjunto(adjunto)
                .estado(estados.inicial(EProcesoEstado.PQRS)).build());
        return aRespuesta(nueva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PqrsResponse> mias() {
        Long personaId = usuarioActual.personaId()
                .orElseThrow(() -> new ReglaNegocioException("Su cuenta no tiene una persona asociada"));
        return pqrs.findByPersonaIdOrderByFechaHoraRadicacionDesc(personaId).stream().map(this::aRespuesta).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PqrsResponse> listar() {
        return pqrs.findAllByOrderByFechaHoraRadicacionDesc().stream().map(this::aRespuesta).toList();
    }

    @Override
    public PqrsResponse atender(Long id, AtenderPqrsRequest s) {
        Pqrs p = pqrs.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("PQRS", id));
        p.cambiarEstado(estados.transicionar(p.getEstado(), NombresEstado.PQRS_RESPONDIDA, null));
        p.responder(s.respuesta().trim());
        notificador.notificar(ETipoEvento.RESPUESTA_PQRS, List.of(p.getPersona()),
                "Su " + p.getTipo().getNombre() + " \"" + p.getAsunto() + "\" ya tiene respuesta.");
        return aRespuesta(p);
    }

    private Persona personaActual() {
        Long personaId = usuarioActual.personaId()
                .orElseThrow(() -> new ReglaNegocioException("Su cuenta no tiene una persona asociada"));
        return personas.findById(personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona", personaId));
    }

    /** Sin fila de configuracion no se calcula vencimiento. */
    private PqrsResponse aRespuesta(Pqrs p) {
        Optional<Integer> plazo = configuracion.findAll().stream().findFirst()
                .map(ConfiguracionGeneral::getPlazoRespuestaPqrsDias);
        return PqrsResponse.de(p, plazo.map(p::estaVencida).orElse(false));
    }

    @Override
    public void eliminar(Long id) {
        Pqrs radicada = pqrs.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("PQRS", id));
        Borrado.exigirEstadoInicial(radicada.getEstado(), "La PQRS");
        pqrs.delete(radicada);
    }
}
