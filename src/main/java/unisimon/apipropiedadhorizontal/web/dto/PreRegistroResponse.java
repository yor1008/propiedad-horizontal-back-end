package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.enums.ETipoVigencia;
import unisimon.apipropiedadhorizontal.domain.model.PreRegistro;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** {@code vigente}: se puede usar hoy (activo y, si es de fecha especifica, sin usar y con la fecha de hoy). */
public record PreRegistroResponse(Long id, Long visitanteId, String visitante, String numeroDocumento,
                                  Long personaId, String residente, Long inmuebleId, String inmueble,
                                  ETipoVigencia tipoVigencia, LocalDate fechaPrevista, String motivo,
                                  String placaVehiculo, String tipoVehiculo, boolean activo, boolean usado,
                                  boolean vigente, LocalDateTime fechaRegistro) {

    public static PreRegistroResponse de(PreRegistro p, LocalDate hoy) {
        return new PreRegistroResponse(p.getId(), p.getVisitante().getId(), p.getVisitante().getNombreCompleto(),
                p.getVisitante().getNumeroDocumento(), p.getPersona().getId(),
                p.getPersona().getNombres() + " " + p.getPersona().getApellidos(), p.getInmueble().getId(),
                p.getInmueble().getTorreBloque() + "-" + p.getInmueble().getNumero(), p.getTipoVigencia(),
                p.getFechaPrevista(), p.getMotivo(), p.getPlacaVehiculo(),
                p.getTipoVehiculo() == null ? null : p.getTipoVehiculo().getNombre(), p.isActivo(), p.isUsado(),
                p.estaVigente(hoy), p.getFechaHoraRegistro());
    }
}
