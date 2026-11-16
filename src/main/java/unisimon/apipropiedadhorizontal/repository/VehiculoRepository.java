package unisimon.apipropiedadhorizontal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.Vehiculo;

/** Vehiculos (CU-012, CU-013). */
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findByPlacaIgnoreCase(String placa);

    /** Operacion de clase del diagrama (Vehiculo.buscarPorPlaca); vive en el repositorio porque una entidad no consulta la base. Ignora espacios y mayusculas. */
    default Optional<Vehiculo> buscarPorPlaca(String placa) {
        return findByPlacaIgnoreCase(placa.trim());
    }
    long countByInmuebleIdAndActivoTrue(Long inmuebleId);
    boolean existsByPersonaId(Long id);
    boolean existsByInmuebleId(Long inmuebleId);
}
