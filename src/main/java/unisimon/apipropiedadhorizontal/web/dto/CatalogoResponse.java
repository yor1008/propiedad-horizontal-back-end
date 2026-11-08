package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;

public record CatalogoResponse(Long id, ETipoCatalogo tipo, String nombre, boolean activo) {

    public static CatalogoResponse de(ValorCatalogo v) {
        return new CatalogoResponse(v.getId(), v.getTipo(), v.getNombre(), v.isActivo());
    }
}
