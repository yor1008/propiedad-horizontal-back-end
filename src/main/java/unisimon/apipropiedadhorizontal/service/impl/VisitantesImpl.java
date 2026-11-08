package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.Visitante;
import unisimon.apipropiedadhorizontal.repository.VisitanteRepository;
import unisimon.apipropiedadhorizontal.service.Visitantes;

@Component
public class VisitantesImpl implements Visitantes {

    private final VisitanteRepository visitantes;

    public VisitantesImpl(VisitanteRepository visitantes) {
        this.visitantes = visitantes;
    }

    @Override
    @Transactional
    public Visitante obtenerOCrear(String nombreCompleto, String numeroDocumento) {
        String documento = numeroDocumento.trim();
        return visitantes.findByNumeroDocumento(documento).orElseGet(() -> visitantes.save(
                Visitante.builder().nombreCompleto(nombreCompleto.trim()).numeroDocumento(documento).build()));
    }
}
