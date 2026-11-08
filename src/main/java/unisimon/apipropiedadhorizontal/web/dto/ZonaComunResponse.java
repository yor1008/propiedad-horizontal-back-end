package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.enums.EPeriodoLimite;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoUsoZona;
import unisimon.apipropiedadhorizontal.domain.model.ZonaComun;

import java.util.List;

public record ZonaComunResponse(Long id, String nombre, String descripcion, int capacidadMaxima, ETipoUsoZona tipoUso,
                                boolean requiereAprobacion, int duracionMaximaMinutos, int anticipacionMinimaHoras,
                                int anticipacionMaximaDias, int limiteCancelacionHoras, int maxReservasPorInmueble,
                                EPeriodoLimite periodoLimite, boolean activa, List<HorarioDto> horarios) {

    public static ZonaComunResponse de(ZonaComun z) {
        return new ZonaComunResponse(z.getId(), z.getNombre(), z.getDescripcion(), z.getCapacidadMaxima(),
                z.getTipoUso(), z.isRequiereAprobacion(), z.getDuracionMaximaMinutos(),
                z.getAnticipacionMinimaHoras(), z.getAnticipacionMaximaDias(), z.getLimiteCancelacionHoras(),
                z.getMaxReservasPorInmueble(), z.getPeriodoLimite(), z.isActiva(),
                z.getHorarios().stream().map(HorarioDto::de).toList());
    }
}
