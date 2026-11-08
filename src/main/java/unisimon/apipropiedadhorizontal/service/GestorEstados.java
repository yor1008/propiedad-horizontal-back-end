package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.domain.enums.EProcesoEstado;
import unisimon.apipropiedadhorizontal.domain.model.Estado;

/** Maquina de estados por datos: valida los cambios contra {@code TransicionEstado}. */
public interface GestorEstados {

    Estado inicial(EProcesoEstado proceso);

    /** Devuelve el estado destino si la transicion existe; 422 si no existe o falta el motivo exigido. */
    Estado transicionar(Estado actual, String nombreDestino, String motivo);

    Estado porNombre(EProcesoEstado proceso, String nombre);
}
