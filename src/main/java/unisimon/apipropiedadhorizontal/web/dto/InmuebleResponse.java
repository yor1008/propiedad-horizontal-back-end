package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.model.Inmueble;

/** {@code propietario} es el nombre del propietario con vinculo vigente (o null). */
public record InmuebleResponse(Long id, String torreBloque, String numero, int piso, String tipo,
                               String propietario, boolean ocupado, boolean activo) {

    public static InmuebleResponse de(Inmueble i, String propietario) {
        return new InmuebleResponse(i.getId(), i.getTorreBloque(), i.getNumero(), i.getPiso(),
                i.getTipo().getNombre(), propietario, i.isOcupado(), i.isActivo());
    }
}
