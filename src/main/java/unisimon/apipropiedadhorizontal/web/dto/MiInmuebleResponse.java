package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.model.Inmueble;

import java.util.List;

/** Inmueble del propietario con los parqueaderos que tiene asignados. */
public record MiInmuebleResponse(Long id, String torreBloque, String numero, int piso, String tipo, boolean ocupado,
                                 boolean activo, List<ParqueaderoResponse> parqueaderos) {

    public static MiInmuebleResponse de(Inmueble i, List<ParqueaderoResponse> parqueaderos) {
        return new MiInmuebleResponse(i.getId(), i.getTorreBloque(), i.getNumero(), i.getPiso(),
                i.getTipo().getNombre(), i.isOcupado(), i.isActivo(), parqueaderos);
    }
}
