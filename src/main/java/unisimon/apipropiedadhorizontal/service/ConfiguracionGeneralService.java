package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.ConfiguracionGeneralRequest;
import unisimon.apipropiedadhorizontal.web.dto.ConfiguracionGeneralResponse;

/** CU-007 Configurar parametros generales del conjunto (Administrador): una unica configuracion. */
public interface ConfiguracionGeneralService {

    /** Si aun no existe la fila, la crea con los valores por defecto. */
    ConfiguracionGeneralResponse obtener();

    ConfiguracionGeneralResponse actualizar(ConfiguracionGeneralRequest solicitud);
}
