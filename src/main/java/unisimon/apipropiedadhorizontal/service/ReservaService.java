package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.web.dto.ReservaResponse;
import unisimon.apipropiedadhorizontal.web.dto.ReservarRequest;

import java.util.List;

/** CU-025 Reservar zona comun y CU-026 Gestionar reservas propias (Residente). */
public interface ReservaService {

    ReservaResponse reservar(ReservarRequest solicitud);

    List<ReservaResponse> mias();

    ReservaResponse cancelar(Long id, String motivo);
}
