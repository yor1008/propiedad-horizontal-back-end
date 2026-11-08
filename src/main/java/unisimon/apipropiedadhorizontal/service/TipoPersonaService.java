package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.TipoPersonaResponse;

import java.util.List;

/** Consulta de tipos de persona (lectura para cualquier autenticado). */
public interface TipoPersonaService {

    /** Solo los activos, ordenados por nombre. */
    List<TipoPersonaResponse> listarActivos();
}
