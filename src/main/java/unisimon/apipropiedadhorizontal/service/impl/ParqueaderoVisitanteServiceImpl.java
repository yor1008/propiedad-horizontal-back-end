package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ECategoriaInmueble;
import unisimon.apipropiedadhorizontal.domain.model.Inmueble;
import unisimon.apipropiedadhorizontal.domain.model.RegistroVisita;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.InmuebleRepository;
import unisimon.apipropiedadhorizontal.repository.RegistroVisitaRepository;
import unisimon.apipropiedadhorizontal.service.ParqueaderoVisitanteService;
import unisimon.apipropiedadhorizontal.web.dto.ParqueaderoResponse;
import unisimon.apipropiedadhorizontal.web.dto.RegistroVisitaResponse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ParqueaderoVisitanteServiceImpl implements ParqueaderoVisitanteService {

    private final RegistroVisitaRepository registros;
    private final InmuebleRepository inmuebles;

    public ParqueaderoVisitanteServiceImpl(RegistroVisitaRepository registros, InmuebleRepository inmuebles) {
        this.registros = registros;
        this.inmuebles = inmuebles;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParqueaderoResponse> disponibles() {
        Set<Long> enUso = registros.findByIngresoAutorizadoTrueAndFechaHoraEntradaIsNotNullAndFechaHoraSalidaIsNull()
                .stream()
                .filter(r -> r.getParqueaderoAsignado() != null)
                .map(r -> r.getParqueaderoAsignado().getId())
                .collect(Collectors.toSet());
        return inmuebles.findByCategoria(ECategoriaInmueble.PARQUEADERO).stream()
                .filter(p -> p.isActivo() && p.getInmuebleAsociado() == null && !enUso.contains(p.getId()))
                .map(ParqueaderoResponse::de)
                .toList();
    }

    @Override
    public RegistroVisitaResponse asignar(Long registroId, Long parqueaderoId) {
        RegistroVisita registro = registros.findById(registroId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Registro de visita", registroId));
        if (!registro.estaDentro()) {
            throw new ReglaNegocioException("Solo se asigna parqueadero a un visitante que esta dentro del conjunto");
        }
        Inmueble parqueadero = inmuebles.findById(parqueaderoId)
                .filter(i -> i.getCategoria() == ECategoriaInmueble.PARQUEADERO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Parqueadero", parqueaderoId));
        if (!parqueadero.isActivo()) {
            throw new ReglaNegocioException("El parqueadero esta inactivo");
        }
        if (parqueadero.getInmuebleAsociado() != null) {
            throw new ReglaNegocioException("Ese parqueadero pertenece a un inmueble; elija uno de visitantes");
        }
        if (registros.existsByParqueaderoAsignadoIdAndIngresoAutorizadoTrueAndFechaHoraSalidaIsNullAndIdNot(
                parqueaderoId, registroId)) {
            throw new ConflictoException("El parqueadero ya esta ocupado por otro visitante");
        }
        registro.asignarParqueadero(parqueadero);
        return RegistroVisitaResponse.de(registro);
    }
}
