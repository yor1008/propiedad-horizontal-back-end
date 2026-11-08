package unisimon.apipropiedadhorizontal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import unisimon.apipropiedadhorizontal.domain.model.RegistroVisita;

/** Historial y control de accesos (CU-016 a CU-021). */
public interface RegistroVisitaRepository
        extends JpaRepository<RegistroVisita, Long>, JpaSpecificationExecutor<RegistroVisita> {

    /** Visitantes que estan dentro del conjunto ahora. */
    List<RegistroVisita> findByIngresoAutorizadoTrueAndFechaHoraEntradaIsNotNullAndFechaHoraSalidaIsNull();
    List<RegistroVisita> findByInmuebleIdOrderByFechaHoraLlegadaDesc(Long inmuebleId);
    boolean existsByResidenteContactadoId(Long id);
    boolean existsByVisitanteIdAndIngresoAutorizadoTrueAndFechaHoraEntradaIsNotNullAndFechaHoraSalidaIsNull(Long visitanteId);
    /** El parqueadero lo ocupa otra visita que sigue dentro (se libera solo al registrar la salida). */
    boolean existsByParqueaderoAsignadoIdAndIngresoAutorizadoTrueAndFechaHoraSalidaIsNullAndIdNot(Long parqueaderoId, Long id);
    boolean existsByInmuebleId(Long inmuebleId);
}
