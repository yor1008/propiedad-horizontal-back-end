package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.model.Inmueble;

/** {@code disponible} = activo y sin inmueble asignado. */
public record ParqueaderoResponse(Long id, String torreBloque, String numero, int piso, String tipo,
                                  Long inmuebleAsociadoId, String inmuebleAsociado, boolean activo,
                                  boolean disponible) {

    public static ParqueaderoResponse de(Inmueble p) {
        Inmueble a = p.getInmuebleAsociado();
        return new ParqueaderoResponse(p.getId(), p.getTorreBloque(), p.getNumero(), p.getPiso(),
                p.getTipo().getNombre(), a == null ? null : a.getId(),
                a == null ? null : a.getTorreBloque() + "-" + a.getNumero(), p.isActivo(),
                p.isActivo() && a == null);
    }
}
