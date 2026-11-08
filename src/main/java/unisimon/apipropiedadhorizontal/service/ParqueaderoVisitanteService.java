package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.ParqueaderoResponse;
import unisimon.apipropiedadhorizontal.web.dto.RegistroVisitaResponse;

import java.util.List;

/** CU-018 Asignar parqueadero de visitante (Vigilante); extiende el ingreso (CU-016). */
public interface ParqueaderoVisitanteService {

    /** Parqueaderos activos, sin inmueble asignado y que ninguna visita dentro esta usando. */
    List<ParqueaderoResponse> disponibles();

    RegistroVisitaResponse asignar(Long registroId, Long parqueaderoId);
}
