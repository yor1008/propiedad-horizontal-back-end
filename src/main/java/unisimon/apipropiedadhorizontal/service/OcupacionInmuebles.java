package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.domain.model.Inmueble;

/** Mantiene {@code Inmueble.ocupado} = hay al menos un residente con vinculo vigente. Lo usan vinculaciones y bajas. */
public interface OcupacionInmuebles {

    /** Debe llamarse dentro de la transaccion del cambio, con los cambios de personas ya enviados a la base. */
    void recalcular(Inmueble inmueble);
}
