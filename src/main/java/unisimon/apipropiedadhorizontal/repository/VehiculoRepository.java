package unisimon.apipropiedadhorizontal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.Vehiculo;

/** Vehiculos (CU-012, CU-013). */
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findByPlacaIgnoreCase(String placa);
    long countByInmuebleIdAndActivoTrue(Long inmuebleId);
    boolean existsByPersonaId(Long id);
    boolean existsByInmuebleId(Long inmuebleId);
}
