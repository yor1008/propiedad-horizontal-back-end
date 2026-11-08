package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.CrearParqueaderoRequest;
import unisimon.apipropiedadhorizontal.web.dto.ParqueaderoResponse;

import java.util.List;

/** CU-014 Gestionar parqueaderos (Administrador). */
public interface ParqueaderoService {

    ParqueaderoResponse crear(CrearParqueaderoRequest solicitud);

    List<ParqueaderoResponse> listar();

    ParqueaderoResponse asignar(Long parqueaderoId, Long inmuebleId);

    ParqueaderoResponse liberar(Long parqueaderoId);

    ParqueaderoResponse desactivar(Long parqueaderoId, String motivo);
}
