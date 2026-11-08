package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.CrearCuentaRequest;
import unisimon.apipropiedadhorizontal.web.dto.CuentaResponse;

import java.util.List;

/** CU-005 Gestionar cuentas de usuario (Administrador). */
public interface CuentaUsuarioService {

    CuentaResponse crear(CrearCuentaRequest solicitud);

    List<CuentaResponse> listar();

    CuentaResponse obtener(Long id);

    CuentaResponse desactivar(Long id);

    CuentaResponse reactivar(Long id);

    /** Borrado fisico de una cuenta; no se puede borrar la propia (422). */
    void eliminar(Long id);
}
