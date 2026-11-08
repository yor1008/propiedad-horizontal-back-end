package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.domain.model.Inmueble;

/** Decide si un inmueble tiene datos asociados que impiden borrarlo fisicamente (regla: no borrar datos con historia). */
public interface HistorialInmueble {

    boolean tieneHistorial(Inmueble inmueble);
}
