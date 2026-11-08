package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.web.dto.CatalogoRequest;
import unisimon.apipropiedadhorizontal.web.dto.CatalogoResponse;

import java.util.List;

/** CU-006 Administrar catalogos. */
public interface CatalogoService {

    List<CatalogoResponse> listar(ETipoCatalogo tipo, boolean soloActivos);

    CatalogoResponse crear(CatalogoRequest solicitud);

    CatalogoResponse renombrar(Long id, String nombre);

    CatalogoResponse desactivar(Long id);

    CatalogoResponse reactivar(Long id);
}
