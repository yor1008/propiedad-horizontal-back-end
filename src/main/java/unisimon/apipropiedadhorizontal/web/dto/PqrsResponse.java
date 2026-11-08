package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.model.Pqrs;

import java.time.LocalDateTime;

public record PqrsResponse(Long id, String tipo, String asunto, String descripcion, String adjunto,
                           LocalDateTime fechaRadicacion, String estado, String respuesta,
                           LocalDateTime fechaRespuesta, boolean vencida, Long personaId, String solicitante,
                           String inmueble) {

    public static PqrsResponse de(Pqrs p, boolean vencida) {
        return new PqrsResponse(p.getId(), p.getTipo().getNombre(), p.getAsunto(), p.getDescripcion(),
                p.getAdjunto(), p.getFechaHoraRadicacion(), p.getEstado().getNombre(), p.getRespuesta(),
                p.getFechaHoraRespuesta(), vencida, p.getPersona().getId(),
                p.getPersona().getNombres() + " " + p.getPersona().getApellidos(),
                p.getInmueble().getTorreBloque() + "-" + p.getInmueble().getNumero());
    }
}
