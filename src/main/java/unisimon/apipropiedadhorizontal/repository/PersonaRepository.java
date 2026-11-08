package unisimon.apipropiedadhorizontal.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.Persona;

/** Personas vinculadas. */
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    List<Persona> findByInmuebleId(Long inmuebleId);
    Optional<Persona> findByTipoDocumentoIdAndNumeroDocumento(Long tipoDocumentoId, String numeroDocumento);
    boolean existsByInmuebleId(Long inmuebleId);
}
