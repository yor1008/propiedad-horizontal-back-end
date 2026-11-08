package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.RegistroVisita;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.RegistroVisitaRepository;
import unisimon.apipropiedadhorizontal.service.HistorialAccesoService;
import unisimon.apipropiedadhorizontal.web.dto.RegistroVisitaResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class HistorialAccesoServiceImpl implements HistorialAccesoService {

    private static final int MAXIMO = 500;

    private final RegistroVisitaRepository registros;

    public HistorialAccesoServiceImpl(RegistroVisitaRepository registros) {
        this.registros = registros;
    }

    @Override
    public List<RegistroVisitaResponse> consultar(LocalDate desde, LocalDate hasta, String numeroDocumento) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new ReglaNegocioException("hasta no puede ser anterior a desde");
        }
        Specification<RegistroVisita> filtro = (r, q, cb) -> cb.conjunction();
        if (desde != null) {
            LocalDateTime inicio = desde.atStartOfDay();
            filtro = filtro.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.<LocalDateTime>get("fechaHoraLlegada"), inicio));
        }
        if (hasta != null) {
            LocalDateTime fin = hasta.plusDays(1).atStartOfDay();
            filtro = filtro.and((r, q, cb) -> cb.lessThan(r.<LocalDateTime>get("fechaHoraLlegada"), fin));
        }
        if (numeroDocumento != null && !numeroDocumento.isBlank()) {
            String documento = numeroDocumento.trim();
            filtro = filtro.and((r, q, cb) -> cb.equal(r.get("visitante").get("numeroDocumento"), documento));
        }
        var pagina = PageRequest.of(0, MAXIMO, Sort.by(Sort.Direction.DESC, "fechaHoraLlegada"));
        return registros.findAll(filtro, pagina).getContent().stream().map(RegistroVisitaResponse::de).toList();
    }
}
