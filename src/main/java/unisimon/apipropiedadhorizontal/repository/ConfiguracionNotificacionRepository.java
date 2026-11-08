package unisimon.apipropiedadhorizontal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionNotificacion;

/** Configuracion de notificaciones por evento. */
public interface ConfiguracionNotificacionRepository extends JpaRepository<ConfiguracionNotificacion, Long> {

    Optional<ConfiguracionNotificacion> findByEvento(ETipoEvento evento);
}
