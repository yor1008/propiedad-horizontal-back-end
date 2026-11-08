package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.ActualizarVehiculoRequest;
import unisimon.apipropiedadhorizontal.web.dto.CrearVehiculoRequest;
import unisimon.apipropiedadhorizontal.web.dto.VehiculoResponse;

import java.util.List;

/** CU-012 vehiculos (Administrador) y CU-013 consulta por placa (Vigilante). */
public interface VehiculoService {

    VehiculoResponse crear(CrearVehiculoRequest solicitud);

    List<VehiculoResponse> listar();

    VehiculoResponse obtener(Long id);

    VehiculoResponse actualizar(Long id, ActualizarVehiculoRequest solicitud);

    VehiculoResponse desactivar(Long id, String motivo);

    VehiculoResponse consultarPorPlaca(String placa);
}
