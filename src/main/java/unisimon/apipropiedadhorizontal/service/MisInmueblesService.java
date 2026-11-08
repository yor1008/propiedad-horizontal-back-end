package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.MiInmuebleResponse;

import java.util.List;

/** CU-011 Consultar inmuebles propios (Propietario). */
public interface MisInmueblesService {

    /** Inmuebles donde la persona del token es propietaria con vinculo vigente (lista vacia si no lo es). */
    List<MiInmuebleResponse> consultar();
}
