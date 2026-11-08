package unisimon.apipropiedadhorizontal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.Notificacion;

/** Notificaciones de una persona (CU-004). */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByPersonaIdOrderByFechaHoraDesc(Long personaId);
    List<Notificacion> findByPersonaIdAndLeidaFalseOrderByFechaHoraDesc(Long personaId);
    long countByPersonaIdAndLeidaFalse(Long personaId);
    boolean existsByPersonaId(Long id);
}
