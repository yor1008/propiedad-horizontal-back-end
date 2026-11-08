package unisimon.apipropiedadhorizontal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.Pqrs;

/** PQRS (CU-029 a CU-031). */
public interface PqrsRepository extends JpaRepository<Pqrs, Long> {

    List<Pqrs> findByPersonaIdOrderByFechaHoraRadicacionDesc(Long personaId);
    List<Pqrs> findAllByOrderByFechaHoraRadicacionDesc();
    boolean existsByPersonaId(Long id);
    boolean existsByInmuebleId(Long inmuebleId);
}
