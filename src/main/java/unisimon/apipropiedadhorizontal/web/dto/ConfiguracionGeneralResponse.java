package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionGeneral;

public record ConfiguracionGeneralResponse(int maxVehiculosPorInmueble, int plazoRespuestaPqrsDias) {

    public static ConfiguracionGeneralResponse de(ConfiguracionGeneral c) {
        return new ConfiguracionGeneralResponse(c.getMaxVehiculosPorInmueble(), c.getPlazoRespuestaPqrsDias());
    }
}
