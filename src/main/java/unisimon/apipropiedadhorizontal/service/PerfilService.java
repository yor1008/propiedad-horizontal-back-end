package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.ActualizarPersonaRequest;
import unisimon.apipropiedadhorizontal.web.dto.PersonaResponse;

/** CU-003 Gestionar datos propios (Persona vinculada): siempre sobre la persona del token. */
public interface PerfilService {

    PersonaResponse obtener();

    PersonaResponse actualizar(ActualizarPersonaRequest solicitud);
}
