package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.model.Correspondencia;

import java.time.LocalDateTime;

/** {@code destinatario} es la persona o, si no hay, "Inmueble T-N". */
public record CorrespondenciaResponse(Long id, String remitente, String destinatario, Long personaId,
                                      Long inmuebleId, String inmueble, String tipo, String descripcion,
                                      LocalDateTime fechaRecepcion, LocalDateTime fechaEntrega, String estado,
                                      String nombreRecibe) {

    public static CorrespondenciaResponse de(Correspondencia c) {
        String inmueble = c.getInmueble().getTorreBloque() + "-" + c.getInmueble().getNumero();
        String destinatario = c.getPersona() == null ? "Inmueble " + inmueble
                : c.getPersona().getNombres() + " " + c.getPersona().getApellidos();
        return new CorrespondenciaResponse(c.getId(), c.getRemitente(), destinatario,
                c.getPersona() == null ? null : c.getPersona().getId(), c.getInmueble().getId(), inmueble,
                c.getTipo().getNombre(), c.getDescripcion(), c.getFechaHoraRecepcion(), c.getFechaHoraEntrega(),
                c.getEstado().getNombre(), c.getNombreRecibe());
    }
}
