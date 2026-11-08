package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.ConteoNotificacionesResponse;
import unisimon.apipropiedadhorizontal.web.dto.NotificacionResponse;

import java.util.List;

/** CU-004 Consultar notificaciones (Persona vinculada), siempre las de la persona del token. */
public interface NotificacionService {

    List<NotificacionResponse> mias(boolean soloNoLeidas);

    ConteoNotificacionesResponse conteoNoLeidas();

    /** 404 si no existe o es de otra persona. */
    NotificacionResponse marcarLeida(Long id);

    /** Marca todas las propias y devuelve el conteo resultante (0). */
    ConteoNotificacionesResponse marcarTodasLeidas();
}
