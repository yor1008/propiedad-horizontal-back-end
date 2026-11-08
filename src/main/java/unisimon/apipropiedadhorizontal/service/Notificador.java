package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.model.Persona;

import java.util.Collection;

/**
 * Publica notificaciones internas cuando ocurre un evento del sistema. Respeta {@code ConfiguracionNotificacion}:
 * si el evento esta desactivado no genera nada; si no tiene configuracion, esta activo por defecto. Debe llamarse
 * dentro de la transaccion del caso de uso que origina el evento.
 */
public interface Notificador {

    void notificar(ETipoEvento evento, Collection<Persona> destinatarios, String mensaje);
}
