package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.ActualizarMantenimientoRequest;
import unisimon.apipropiedadhorizontal.web.dto.MantenimientoResponse;
import unisimon.apipropiedadhorizontal.web.dto.ProgramarMantenimientoRequest;

import java.util.List;

/** CU-028 Gestionar mantenimientos (Administrador). */
public interface MantenimientoService {

    MantenimientoResponse programar(ProgramarMantenimientoRequest solicitud);

    List<MantenimientoResponse> listar();

    MantenimientoResponse obtener(Long id);

    MantenimientoResponse actualizar(Long id, ActualizarMantenimientoRequest solicitud);

    MantenimientoResponse iniciar(Long id);

    MantenimientoResponse finalizar(Long id, String observaciones);

    MantenimientoResponse cancelar(Long id, String motivo);

    /** Borrado fisico solo en estado inicial; despues, 409 (cancelar). */
    void eliminar(Long id);
}
