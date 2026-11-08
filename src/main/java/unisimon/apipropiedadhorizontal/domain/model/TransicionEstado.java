package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Cambio de estado permitido entre dos {@link Estado} del mismo proceso. */
@Entity
@Table(name = "transicion_estado",
        uniqueConstraints = @UniqueConstraint(name = "uk_transicion_origen_destino",
                columnNames = {"estado_origen_id", "estado_destino_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class TransicionEstado extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_origen_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transicion_origen"))
    private Estado estadoOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_destino_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transicion_destino"))
    private Estado estadoDestino;

    @Column(name = "requiere_motivo", nullable = false)
    private boolean requiereMotivo;
}
