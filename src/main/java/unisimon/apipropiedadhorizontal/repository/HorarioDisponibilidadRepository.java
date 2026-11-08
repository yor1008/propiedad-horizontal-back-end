package unisimon.apipropiedadhorizontal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.HorarioDisponibilidad;

/** Horarios de zonas comunes. */
public interface HorarioDisponibilidadRepository extends JpaRepository<HorarioDisponibilidad, Long> {

    List<HorarioDisponibilidad> findByZonaComunId(Long zonaComunId);
}
