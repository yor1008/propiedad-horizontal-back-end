package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.NombresEstado;
import unisimon.apipropiedadhorizontal.domain.enums.EProcesoEstado;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.Correspondencia;
import unisimon.apipropiedadhorizontal.domain.model.Inmueble;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.CorrespondenciaRepository;
import unisimon.apipropiedadhorizontal.repository.InmuebleRepository;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.security.UsuarioActual;
import unisimon.apipropiedadhorizontal.service.CorrespondenciaService;
import unisimon.apipropiedadhorizontal.service.GestorEstados;
import unisimon.apipropiedadhorizontal.service.Notificador;
import unisimon.apipropiedadhorizontal.web.dto.CorrespondenciaResponse;
import unisimon.apipropiedadhorizontal.web.dto.EntregarCorrespondenciaRequest;
import unisimon.apipropiedadhorizontal.web.dto.RegistrarCorrespondenciaRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class CorrespondenciaServiceImpl implements CorrespondenciaService {

    private final CorrespondenciaRepository correspondencias;
    private final InmuebleRepository inmuebles;
    private final PersonaRepository personas;
    private final ValorCatalogoRepository catalogos;
    private final GestorEstados estados;
    private final UsuarioActual usuarioActual;
    private final Notificador notificador;

    public CorrespondenciaServiceImpl(CorrespondenciaRepository correspondencias, InmuebleRepository inmuebles,
                                      PersonaRepository personas, ValorCatalogoRepository catalogos,
                                      GestorEstados estados, UsuarioActual usuarioActual, Notificador notificador) {
        this.correspondencias = correspondencias;
        this.inmuebles = inmuebles;
        this.personas = personas;
        this.catalogos = catalogos;
        this.estados = estados;
        this.usuarioActual = usuarioActual;
        this.notificador = notificador;
    }

    @Override
    public CorrespondenciaResponse registrar(RegistrarCorrespondenciaRequest s) {
        Inmueble inmueble = inmuebles.findById(s.inmuebleId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Inmueble", s.inmuebleId()));
        Persona persona = null;
        if (s.personaId() != null) {
            persona = personas.findById(s.personaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Persona", s.personaId()));
            if (!persona.vinculoEstaVigente() || !inmueble.equals(persona.getInmueble())) {
                throw new ReglaNegocioException("El destinatario no tiene vinculo vigente con ese inmueble");
            }
        }
        ValorCatalogo tipo = catalogos.findById(s.tipoId())
                .filter(v -> v.getTipo() == ETipoCatalogo.TIPO_CORRESPONDENCIA)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de correspondencia", s.tipoId()));
        if (!tipo.isActivo()) {
            throw new ReglaNegocioException("El tipo de correspondencia esta inactivo");
        }
        Correspondencia c = correspondencias.save(Correspondencia.builder()
                .inmueble(inmueble).persona(persona).tipo(tipo)
                .remitente(s.remitente()).descripcion(s.descripcion())
                .estado(estados.inicial(EProcesoEstado.CORRESPONDENCIA)).build());
        List<Persona> destinatarios = persona != null ? List.of(persona)
                : personas.findByInmuebleId(inmueble.getId()).stream().filter(Persona::vinculoEstaVigente).toList();
        String deQuien = s.remitente() == null || s.remitente().isBlank() ? "" : " de " + s.remitente().trim();
        notificador.notificar(ETipoEvento.CORRESPONDENCIA_RECIBIDA, destinatarios,
                "Llegó correspondencia (" + tipo.getNombre() + ")" + deQuien + ". Puede recogerla en portería.");
        return CorrespondenciaResponse.de(c);
    }

    @Override
    public CorrespondenciaResponse entregar(Long id, EntregarCorrespondenciaRequest s) {
        Correspondencia c = correspondencias.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Correspondencia", id));
        c.cambiarEstado(estados.transicionar(c.getEstado(), NombresEstado.CORRESPONDENCIA_ENTREGADA, null));
        c.registrarEntrega(s.nombreRecibe().trim(), s.documentoRecibe().trim());
        return CorrespondenciaResponse.de(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorrespondenciaResponse> listar(Long personaId) {
        List<Correspondencia> lista = personaId == null
                ? correspondencias.findAllByOrderByFechaHoraRecepcionDesc()
                : correspondencias.findByPersonaIdOrderByFechaHoraRecepcionDesc(personaId);
        return lista.stream().map(CorrespondenciaResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorrespondenciaResponse> mias() {
        Long personaId = usuarioActual.personaId()
                .orElseThrow(() -> new ReglaNegocioException("Su cuenta no tiene una persona asociada"));
        Persona persona = personas.findById(personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona", personaId));
        List<Correspondencia> lista = new ArrayList<>(
                correspondencias.findByPersonaIdOrderByFechaHoraRecepcionDesc(personaId));
        if (persona.vinculoEstaVigente()) {
            lista.addAll(correspondencias.findByInmuebleIdAndPersonaIsNullOrderByFechaHoraRecepcionDesc(
                    persona.getInmueble().getId()));
        }
        return lista.stream()
                .sorted(Comparator.comparing(Correspondencia::getFechaHoraRecepcion).reversed())
                .map(CorrespondenciaResponse::de).toList();
    }

    @Override
    public void eliminar(Long id) {
        Correspondencia pieza = correspondencias.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Correspondencia", id));
        Borrado.exigirEstadoInicial(pieza.getEstado(), "La correspondencia");
        correspondencias.delete(pieza);
    }
}
