package unisimon.apipropiedadhorizontal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.enums.EProcesoEstado;
import unisimon.apipropiedadhorizontal.domain.model.Estado;

/** Estados por proceso. */
public interface EstadoRepository extends JpaRepository<Estado, Long> {

    Optional<Estado> findByProcesoAndEsInicialTrue(EProcesoEstado proceso);
    Optional<Estado> findByProcesoAndNombre(EProcesoEstado proceso, String nombre);
}
