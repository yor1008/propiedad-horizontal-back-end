package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.model.Persona;

/** Contacto = telefono + correo; estado = activa. */
public record PersonaResponse(Long id, String nombres, String apellidos, String tipoDocumento, String numeroDocumento,
                              Long tipoPersonaId, String tipoPersona, String telefono, String correo,
                              boolean activa, Long inmuebleId, boolean vinculoVigente) {

    public static PersonaResponse de(Persona p) {
        return new PersonaResponse(p.getId(), p.getNombres(), p.getApellidos(), p.getTipoDocumento().getNombre(),
                p.getNumeroDocumento(), p.getTipoPersona().getId(), p.getTipoPersona().getNombre(),
                p.getTelefono(), p.getCorreo(), p.isActiva(),
                p.getInmueble() == null ? null : p.getInmueble().getId(), p.vinculoEstaVigente());
    }
}
