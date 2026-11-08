package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.CrearInmuebleRequest;
import unisimon.apipropiedadhorizontal.web.dto.InmuebleResponse;
import unisimon.apipropiedadhorizontal.web.dto.MotivoRequest;
import unisimon.apipropiedadhorizontal.web.dto.PersonaResponse;
import unisimon.apipropiedadhorizontal.web.dto.VincularPersonaRequest;

import java.util.List;

/** CU-009 inmuebles y CU-010 vinculaciones (Administrador). */
public interface InmuebleService {

    InmuebleResponse crear(CrearInmuebleRequest solicitud);

    List<InmuebleResponse> listar();

    InmuebleResponse obtener(Long id);

    /** Corrige torre, numero, piso y tipo (mismo cuerpo que crear). Solo unidades privadas: un parqueadero da 404. */
    InmuebleResponse actualizar(Long id, CrearInmuebleRequest solicitud);

    /** Baja logica con motivo; 422 si aun tiene personas con vinculo vigente. */
    InmuebleResponse desactivar(Long id, String motivo);

    InmuebleResponse reactivar(Long id);

    /** Borrado fisico, solo si el inmueble no tiene historia (personas, vehiculos, PQRS, etc.); si no, 409. */
    void eliminar(Long id);

    /** Personas vinculadas (vigentes) al inmueble. */
    List<PersonaResponse> personasDe(Long inmuebleId);

    PersonaResponse vincular(Long inmuebleId, VincularPersonaRequest solicitud);

    PersonaResponse finalizarVinculo(Long personaId, MotivoRequest solicitud);
}
