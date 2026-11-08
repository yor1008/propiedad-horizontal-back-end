package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.web.dto.ConfiguracionNotificacionRequest;
import unisimon.apipropiedadhorizontal.web.dto.ConfiguracionNotificacionResponse;

import java.util.List;

/** CU-007 Configurar notificaciones por evento (Administrador). */
public interface ConfiguracionNotificacionService {

    /** Un elemento por cada evento; uno sin fila se muestra con el valor por defecto (activa, sin correo). */
    List<ConfiguracionNotificacionResponse> listar();

    ConfiguracionNotificacionResponse actualizar(ETipoEvento evento, ConfiguracionNotificacionRequest solicitud);
}
