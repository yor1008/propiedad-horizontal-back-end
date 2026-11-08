package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.RegistroVisitaResponse;

import java.time.LocalDate;
import java.util.List;

/** CU-021 Consultar historial de accesos (Administrador o Vigilante). */
public interface HistorialAccesoService {

    /** Filtros opcionales por dia de llegada (inclusive) y documento; maximo 500 registros, los mas recientes primero. */
    List<RegistroVisitaResponse> consultar(LocalDate desde, LocalDate hasta, String numeroDocumento);
}
