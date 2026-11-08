package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.model.TipoPersona;

/** Para cargar el selector de tipo de persona sin ids fijos en el front. */
public record TipoPersonaResponse(Long id, String nombre, String descripcion, boolean esPropietario,
                                  boolean esResidente) {

    public static TipoPersonaResponse de(TipoPersona t) {
        return new TipoPersonaResponse(t.getId(), t.getNombre(), t.getDescripcion(), t.isEsPropietario(),
                t.isEsResidente());
    }
}
