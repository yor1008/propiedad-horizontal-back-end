package unisimon.apipropiedadhorizontal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.enums.ECategoriaInmueble;
import unisimon.apipropiedadhorizontal.domain.model.Inmueble;

/** Inmuebles y parqueaderos. */
public interface InmuebleRepository extends JpaRepository<Inmueble, Long> {

    List<Inmueble> findByInmuebleAsociadoId(Long inmuebleId);
    List<Inmueble> findByCategoria(ECategoriaInmueble categoria);
    boolean existsByTorreBloqueAndNumero(String torreBloque, String numero);
    boolean existsByInmuebleAsociadoId(Long inmuebleId);
    boolean existsByTorreBloqueAndNumeroAndIdNot(String torreBloque, String numero, Long id);
}
