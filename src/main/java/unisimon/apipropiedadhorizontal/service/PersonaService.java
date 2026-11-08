package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.ActualizarPersonaRequest;
import unisimon.apipropiedadhorizontal.web.dto.CrearPersonaRequest;
import unisimon.apipropiedadhorizontal.web.dto.PersonaResponse;

import java.util.List;

/** CU-008 Gestionar personas (Administrador). */
public interface PersonaService {

    PersonaResponse crear(CrearPersonaRequest solicitud);

    List<PersonaResponse> listar();

    PersonaResponse obtener(Long id);

    PersonaResponse actualizar(Long id, ActualizarPersonaRequest solicitud);

    PersonaResponse desactivar(Long id, String motivo);

    PersonaResponse reactivar(Long id);

    /** Borrado fisico, solo si la persona no tiene historia (cuenta, vinculo, vehiculos, PQRS, etc.); si no, 409. */
    void eliminar(Long id);
}
