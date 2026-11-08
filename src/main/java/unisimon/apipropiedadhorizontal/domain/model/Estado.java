package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import unisimon.apipropiedadhorizontal.domain.enums.EProcesoEstado;

/**
 * Estado de una maquina de estados parametrizable por proceso (correspondencia, reserva,
 * mantenimiento, PQRS). Las transiciones validas viven en {@link TransicionEstado}.
 */
@Entity
@Table(name = "estado",
        uniqueConstraints = @UniqueConstraint(name = "uk_estado_proceso_nombre", columnNames = {"proceso", "nombre"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Estado extends EntidadBase {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EProcesoEstado proceso;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "es_inicial", nullable = false)
    private boolean esInicial;

    @Column(name = "es_final", nullable = false)
    private boolean esFinal;
}
