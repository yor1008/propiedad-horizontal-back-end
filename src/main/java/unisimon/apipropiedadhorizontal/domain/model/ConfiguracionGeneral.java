package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Parametros globales del conjunto. Se espera una unica fila. */
@Entity
@Table(name = "configuracion_general")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ConfiguracionGeneral extends EntidadBase {

    @Column(name = "max_vehiculos_por_inmueble", nullable = false)
    private int maxVehiculosPorInmueble;

    @Column(name = "plazo_respuesta_pqrs_dias", nullable = false)
    private int plazoRespuestaPqrsDias;

    public void actualizar(int maxVehiculos, int plazoPqrs) {
        this.maxVehiculosPorInmueble = maxVehiculos;
        this.plazoRespuestaPqrsDias = plazoPqrs;
    }
}
