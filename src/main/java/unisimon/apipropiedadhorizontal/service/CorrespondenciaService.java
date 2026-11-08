package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.CorrespondenciaResponse;
import unisimon.apipropiedadhorizontal.web.dto.EntregarCorrespondenciaRequest;
import unisimon.apipropiedadhorizontal.web.dto.RegistrarCorrespondenciaRequest;

import java.util.List;

/** CU-022 gestionar (Vigilante) y CU-023 consultar (Persona vinculada). */
public interface CorrespondenciaService {

    CorrespondenciaResponse registrar(RegistrarCorrespondenciaRequest solicitud);

    CorrespondenciaResponse entregar(Long id, EntregarCorrespondenciaRequest solicitud);

    /** Todas, o solo las dirigidas a la persona indicada. */
    List<CorrespondenciaResponse> listar(Long personaId);

    /** Las de la persona autenticada y las dirigidas a su inmueble vigente. */
    List<CorrespondenciaResponse> mias();

    /** Borrado fisico solo de correspondencia sin entregar; despues, 409. */
    void eliminar(Long id);
}
