package unisimon.apipropiedadhorizontal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.TransicionEstado;

/** Transiciones permitidas entre estados. */
public interface TransicionEstadoRepository extends JpaRepository<TransicionEstado, Long> {

    Optional<TransicionEstado> findByEstadoOrigenIdAndEstadoDestinoId(Long origenId, Long destinoId);
    void deleteByEstadoOrigenIdOrEstadoDestinoId(Long origenId, Long destinoId);
}
