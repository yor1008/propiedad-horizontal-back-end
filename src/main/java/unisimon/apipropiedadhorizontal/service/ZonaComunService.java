package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.DisponibilidadResponse;
import unisimon.apipropiedadhorizontal.web.dto.ZonaComunRequest;
import unisimon.apipropiedadhorizontal.web.dto.ZonaComunResponse;

import java.time.LocalDate;
import java.util.List;

/** CU-024 Gestionar zonas comunes (Administrador); la consulta la hace cualquier autenticado. */
public interface ZonaComunService {

    ZonaComunResponse crear(ZonaComunRequest solicitud);

    ZonaComunResponse actualizar(Long id, ZonaComunRequest solicitud);

    List<ZonaComunResponse> listar(boolean soloActivas);

    ZonaComunResponse obtener(Long id);

    ZonaComunResponse desactivar(Long id, String motivo);

    ZonaComunResponse reactivar(Long id);

    DisponibilidadResponse disponibilidad(Long id, LocalDate fecha);

    /** Borrado fisico (con sus horarios) solo sin reservas ni mantenimientos; si no, 409 (desactivar). */
    void eliminar(Long id);
}
