package unisimon.apipropiedadhorizontal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.Mantenimiento;

/** Mantenimientos (CU-028). */
public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    List<Mantenimiento> findByZonaComunId(Long zonaComunId);
    List<Mantenimiento> findAllByOrderByFechaHoraInicioDesc();

    /** Mantenimientos de la zona que aun bloquean su uso (estado no final: programado o en curso). */
    List<Mantenimiento> findByZonaComunIdAndEstadoEsFinalFalse(Long zonaComunId);
}
