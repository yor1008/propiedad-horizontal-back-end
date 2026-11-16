package unisimon.apipropiedadhorizontal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.TipoPersona;

/** Tipos de persona. */
public interface TipoPersonaRepository extends JpaRepository<TipoPersona, Long> {

    List<TipoPersona> findByActivoTrueOrderByNombre();

    /** Operacion de clase del diagrama (TipoPersona.listarActivos); vive en el repositorio porque una entidad no consulta la base. */
    default List<TipoPersona> listarActivos() {
        return findByActivoTrueOrderByNombre();
    }
    boolean existsByNombreIgnoreCase(String nombre);
}
