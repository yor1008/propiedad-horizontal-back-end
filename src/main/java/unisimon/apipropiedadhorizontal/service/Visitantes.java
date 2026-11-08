package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.domain.model.Visitante;

/** Busca un visitante por documento o lo registra si no existe (lo usan pre-registros e ingresos sin pre-registro). */
public interface Visitantes {

    /** Si el documento ya existe se conserva el nombre registrado. */
    Visitante obtenerOCrear(String nombreCompleto, String numeroDocumento);
}
