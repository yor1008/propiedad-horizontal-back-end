package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.IngresoSinPreRegistroRequest;
import unisimon.apipropiedadhorizontal.web.dto.InmuebleConResidentesResponse;
import unisimon.apipropiedadhorizontal.web.dto.PreRegistroResponse;
import unisimon.apipropiedadhorizontal.web.dto.RegistroVisitaResponse;

import java.util.List;

/** CU-016 Autorizar ingreso de visitante y CU-017 Autorizar ingreso sin pre-registro (Vigilante). */
public interface IngresoVisitanteService {

    /** Pre-registros de ese documento que se pueden usar hoy. */
    List<PreRegistroResponse> preRegistrosVigentes(String numeroDocumento);

    RegistroVisitaResponse autorizarConPreRegistro(Long preRegistroId);

    RegistroVisitaResponse autorizarSinPreRegistro(IngresoSinPreRegistroRequest solicitud);

    /** Unidades con residentes vigentes, para elegir a quien contactar. */
    List<InmuebleConResidentesResponse> directorio();
}
