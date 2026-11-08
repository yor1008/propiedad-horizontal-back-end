package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.CrearPreRegistroRequest;
import unisimon.apipropiedadhorizontal.web.dto.PreRegistroResponse;

import java.util.List;

/** CU-015 Gestionar pre-registros de visitantes (Residente), siempre sobre la persona del token. */
public interface PreRegistroService {

    PreRegistroResponse crear(CrearPreRegistroRequest solicitud);

    List<PreRegistroResponse> mios();

    PreRegistroResponse cancelar(Long id);
}
