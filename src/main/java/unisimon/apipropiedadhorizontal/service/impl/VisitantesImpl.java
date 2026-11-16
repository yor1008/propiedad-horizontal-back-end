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
        return visitantes.buscarPorDocumento(numeroDocumento).orElseGet(() -> {
            Visitante nuevo = Visitante.builder().build();
            nuevo.registrar(nombreCompleto.trim(), numeroDocumento.trim());
            return visitantes.save(nuevo);
        });
    }
}
