package unisimon.apipropiedadhorizontal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.PreRegistro;

/** Pre-registros de visitantes (CU-015). */
public interface PreRegistroRepository extends JpaRepository<PreRegistro, Long> {

    List<PreRegistro> findByPersonaIdAndActivoTrue(Long personaId);
    List<PreRegistro> findByPersonaIdOrderByFechaHoraRegistroDesc(Long personaId);
    List<PreRegistro> findByVisitanteIdAndActivoTrue(Long visitanteId);
    List<PreRegistro> findByVisitanteIdAndInmuebleIdAndActivoTrue(Long visitanteId, Long inmuebleId);
    boolean existsByPersonaId(Long id);
    boolean existsByInmuebleId(Long inmuebleId);
}
