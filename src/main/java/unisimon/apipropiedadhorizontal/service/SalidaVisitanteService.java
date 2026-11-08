package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.RegistroVisitaResponse;

import java.util.List;

/** CU-019 Registrar salida de visitante y CU-020 Consultar visitantes dentro del conjunto (Vigilante). */
public interface SalidaVisitanteService {

    RegistroVisitaResponse registrarSalida(Long registroId);

    /** Visitantes dentro ahora, los que entraron mas recientemente primero. */
    List<RegistroVisitaResponse> dentro();
}
