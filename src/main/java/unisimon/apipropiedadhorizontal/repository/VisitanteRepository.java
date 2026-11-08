package unisimon.apipropiedadhorizontal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.Visitante;

/** Visitantes registrados. */
public interface VisitanteRepository extends JpaRepository<Visitante, Long> {

    Optional<Visitante> findByNumeroDocumento(String numeroDocumento);
}
