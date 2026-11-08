package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.RegistroVisita;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.RegistroVisitaRepository;
import unisimon.apipropiedadhorizontal.service.SalidaVisitanteService;
import unisimon.apipropiedadhorizontal.web.dto.RegistroVisitaResponse;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class SalidaVisitanteServiceImpl implements SalidaVisitanteService {

    private final RegistroVisitaRepository registros;

    public SalidaVisitanteServiceImpl(RegistroVisitaRepository registros) {
        this.registros = registros;
    }

    @Override
    public RegistroVisitaResponse registrarSalida(Long registroId) {
        RegistroVisita registro = registros.findById(registroId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Registro de visita", registroId));
        if (!registro.estaDentro()) {
            throw new ReglaNegocioException("El visitante no se encuentra dentro del conjunto");
        }
        registro.registrarSalida();
        return RegistroVisitaResponse.de(registro);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroVisitaResponse> dentro() {
        return registros.findByIngresoAutorizadoTrueAndFechaHoraEntradaIsNotNullAndFechaHoraSalidaIsNull().stream()
                .sorted(Comparator.comparing(RegistroVisita::getFechaHoraEntrada).reversed())
                .map(RegistroVisitaResponse::de)
                .toList();
    }
}
