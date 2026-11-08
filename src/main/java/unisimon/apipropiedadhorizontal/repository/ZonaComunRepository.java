package unisimon.apipropiedadhorizontal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.ZonaComun;

/** Zonas comunes (CU-024). */
public interface ZonaComunRepository extends JpaRepository<ZonaComun, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
    List<ZonaComun> findAllByOrderByNombre();
    List<ZonaComun> findByActivaTrueOrderByNombre();
}
