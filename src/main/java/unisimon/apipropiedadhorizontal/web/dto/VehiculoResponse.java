package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.model.Vehiculo;

public record VehiculoResponse(Long id, String placa, String tipo, String marca, String color, boolean activo,
                               Long personaId, String responsable, Long inmuebleId, String inmueble) {

    public static VehiculoResponse de(Vehiculo v) {
        return new VehiculoResponse(v.getId(), v.getPlaca(), v.getTipo().getNombre(), v.getMarca(), v.getColor(),
                v.isActivo(), v.getPersona().getId(),
                v.getPersona().getNombres() + " " + v.getPersona().getApellidos(),
                v.getInmueble().getId(), v.getInmueble().getTorreBloque() + "-" + v.getInmueble().getNumero());
    }
}
