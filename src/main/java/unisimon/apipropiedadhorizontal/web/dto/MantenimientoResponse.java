package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.model.Mantenimiento;

import java.time.LocalDateTime;

public record MantenimientoResponse(Long id, Long zonaComunId, String zonaComun, String tipo, String elementoArea,
                                    String descripcion, LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin,
                                    String responsable, String observaciones, String motivoCancelacion,
                                    String estado) {

    public static MantenimientoResponse de(Mantenimiento m) {
        return new MantenimientoResponse(m.getId(),
                m.getZonaComun() == null ? null : m.getZonaComun().getId(),
                m.getZonaComun() == null ? null : m.getZonaComun().getNombre(),
                m.getTipo().getNombre(), m.getElementoArea(), m.getDescripcion(), m.getFechaHoraInicio(),
                m.getFechaHoraFin(), m.getResponsable(), m.getObservaciones(), m.getMotivoCancelacion(),
                m.getEstado().getNombre());
    }
}
