package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.AtenderPqrsRequest;
import unisimon.apipropiedadhorizontal.web.dto.PqrsResponse;
import unisimon.apipropiedadhorizontal.web.dto.RadicarPqrsRequest;

import java.util.List;

/** CU-029 radicar, CU-030 consultar propias (Persona vinculada), CU-031 atender (Administrador). */
public interface PqrsService {

    PqrsResponse radicar(RadicarPqrsRequest solicitud);

    List<PqrsResponse> mias();

    List<PqrsResponse> listar();

    PqrsResponse atender(Long id, AtenderPqrsRequest solicitud);

    /** Borrado fisico solo de una PQRS sin atender; despues, 409. */
    void eliminar(Long id);
}
