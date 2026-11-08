package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.domain.model.Persona;

/** Decide si una persona tiene datos asociados que impiden borrarla fisicamente (regla: no borrar datos con historia). */
public interface HistorialPersona {

    boolean tieneHistorial(Persona persona);
}
