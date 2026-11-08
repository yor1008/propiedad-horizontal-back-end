package unisimon.apipropiedadhorizontal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.Correspondencia;

/** Correspondencia (CU-022, CU-023). */
public interface CorrespondenciaRepository extends JpaRepository<Correspondencia, Long> {

    List<Correspondencia> findByInmuebleIdAndFechaHoraEntregaIsNull(Long inmuebleId);
    List<Correspondencia> findAllByOrderByFechaHoraRecepcionDesc();
    List<Correspondencia> findByPersonaIdOrderByFechaHoraRecepcionDesc(Long personaId);
    List<Correspondencia> findByInmuebleIdAndPersonaIsNullOrderByFechaHoraRecepcionDesc(Long inmuebleId);
    boolean existsByPersonaId(Long id);
    boolean existsByInmuebleId(Long inmuebleId);
}
