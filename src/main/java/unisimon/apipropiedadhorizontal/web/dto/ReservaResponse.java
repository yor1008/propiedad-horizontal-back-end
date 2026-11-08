package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.model.Reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** {@code puedeCancelar}: el residente aun puede cancelarla (no es final y esta dentro del limite de la zona). */
public record ReservaResponse(Long id, Long zonaId, String zona, Long personaId, String solicitante,
                              Long inmuebleId, String inmueble, LocalDate fecha, LocalTime horaInicio,
                              LocalTime horaFin, int numeroAsistentes, String motivo, String condicionesUso,
                              String motivoRechazoCancelacion, String estado, LocalDateTime fechaRegistro,
                              boolean puedeCancelar) {

    public static ReservaResponse de(Reserva r) {
        return new ReservaResponse(r.getId(), r.getZonaComun().getId(), r.getZonaComun().getNombre(),
                r.getPersona().getId(), r.getPersona().getNombres() + " " + r.getPersona().getApellidos(),
                r.getInmueble().getId(), r.getInmueble().getTorreBloque() + "-" + r.getInmueble().getNumero(),
                r.getFecha(), r.getHoraInicio(), r.getHoraFin(), r.getNumeroAsistentes(), r.getMotivo(),
                r.getCondicionesUso(), r.getMotivoRechazoCancelacion(), r.getEstado().getNombre(),
                r.getFechaHoraRegistro(), !r.getEstado().isEsFinal() && r.puedeCancelarResidente());
    }
}
