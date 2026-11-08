package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.ReservaResponse;

import java.util.List;

/** CU-027 Gestionar reservas (Administrador). */
public interface GestionReservaService {

    List<ReservaResponse> listar();

    ReservaResponse aprobar(Long id, String condiciones);

    ReservaResponse rechazar(Long id, String motivo);

    /** Cancelacion hecha por el administrador (sin el limite de horas del residente). */
    ReservaResponse anular(Long id, String motivo);

    ReservaResponse finalizar(Long id);

    /** Borrado fisico solo de una reserva pendiente; despues, 409 (anular). */
    void eliminar(Long id);
}
