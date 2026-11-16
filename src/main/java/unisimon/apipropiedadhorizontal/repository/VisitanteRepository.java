package unisimon.apipropiedadhorizontal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.Visitante;

/** Visitantes registrados. */
public interface VisitanteRepository extends JpaRepository<Visitante, Long> {

    Optional<Visitante> findByNumeroDocumento(String numeroDocumento);

    /** Operacion de clase del diagrama (Visitante.buscarPorDocumento); vive en el repositorio porque una entidad no consulta la base. Ignora espacios alrededor. */
    default Optional<Visitante> buscarPorDocumento(String documento) {
        return findByNumeroDocumento(documento.trim());
    }
}
