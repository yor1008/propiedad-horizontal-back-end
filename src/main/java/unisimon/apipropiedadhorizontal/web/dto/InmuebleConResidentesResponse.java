package unisimon.apipropiedadhorizontal.web.dto;

import java.util.List;

/** Directorio de porteria: unidades con residentes vigentes, para elegir a quien contactar por un visitante. */
public record InmuebleConResidentesResponse(Long id, String torreBloque, String numero, List<Residente> residentes) {

    public record Residente(Long id, String nombre, String telefono) {
    }
}
