package unisimon.apipropiedadhorizontal.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;

/** Catalogos parametrizables (CU-006). */
public interface ValorCatalogoRepository extends JpaRepository<ValorCatalogo, Long> {

    List<ValorCatalogo> findByTipoOrderByNombre(ETipoCatalogo tipo);
    List<ValorCatalogo> findByTipoAndActivoTrueOrderByNombre(ETipoCatalogo tipo);
    boolean existsByTipoAndNombreIgnoreCase(ETipoCatalogo tipo, String nombre);
}
