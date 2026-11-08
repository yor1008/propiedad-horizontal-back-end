package unisimon.apipropiedadhorizontal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionGeneral;

/** Configuracion global (fila unica). */
public interface ConfiguracionGeneralRepository extends JpaRepository<ConfiguracionGeneral, Long> {
}
