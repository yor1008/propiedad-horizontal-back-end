package unisimon.apipropiedadhorizontal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.enums.EProcesoEstado;
import unisimon.apipropiedadhorizontal.domain.model.Estado;

/** Estados por proceso. */
public interface EstadoRepository extends JpaRepository<Estado, Long> {

    Optional<Estado> findByProcesoAndEsInicialTrue(EProcesoEstado proceso);

    /** Operacion de clase del diagrama (Estado.obtenerInicial); vive en el repositorio porque una entidad no consulta la base. */
    default Optional<Estado> obtenerInicial(EProcesoEstado proceso) {
        return findByProcesoAndEsInicialTrue(proceso);
    }
    Optional<Estado> findByProcesoAndNombre(EProcesoEstado proceso, String nombre);
}
