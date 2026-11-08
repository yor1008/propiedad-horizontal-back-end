package unisimon.apipropiedadhorizontal.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import unisimon.apipropiedadhorizontal.domain.model.Reserva;

/** Reservas (CU-025 a CU-027). */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByPersonaIdOrderByFechaDesc(Long personaId);
    List<Reserva> findAllByOrderByFechaDescHoraInicioDesc();
    List<Reserva> findByZonaComunIdAndFechaAndEstadoEsFinalFalseOrderByHoraInicio(Long zonaId, LocalDate fecha);
    boolean existsByEstadoId(Long estadoId);
    List<Reserva> findByZonaComunIdAndFechaBetweenAndEstadoEsFinalFalse(Long zonaId, LocalDate desde, LocalDate hasta);

    /** Reservas de la zona y fecha cuyo horario se cruza con [inicio, fin) y siguen vigentes (estado no final). */
    @Query("""
            select r from Reserva r
            where r.zonaComun.id = :zonaId and r.fecha = :fecha
              and r.horaInicio < :fin and :inicio < r.horaFin
              and r.estado.esFinal = false
            """)
    List<Reserva> buscarTraslapadas(@Param("zonaId") Long zonaId, @Param("fecha") LocalDate fecha,
                                    @Param("inicio") LocalTime inicio, @Param("fin") LocalTime fin);

    /** Reservas del inmueble en la zona dentro del periodo, sin contar las rechazadas o canceladas. */
    @Query("""
            select count(r) from Reserva r
            where r.inmueble.id = :inmuebleId and r.zonaComun.id = :zonaId
              and r.fecha between :desde and :hasta
              and r.estado.nombre not in :excluidos
            """)
    long contarEnPeriodo(@Param("inmuebleId") Long inmuebleId, @Param("zonaId") Long zonaId,
                         @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                         @Param("excluidos") Collection<String> excluidos);
    boolean existsByPersonaId(Long id);
    boolean existsByInmuebleId(Long inmuebleId);
}
