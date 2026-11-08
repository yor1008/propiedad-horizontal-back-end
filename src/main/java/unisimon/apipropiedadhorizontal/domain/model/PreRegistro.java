package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoVigencia;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Autorizacion anticipada que un residente registra para un visitante. */
@Entity
@Table(name = "pre_registro")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class PreRegistro extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pre_registro_persona"))
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitante_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pre_registro_visitante"))
    private Visitante visitante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmueble_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pre_registro_inmueble"))
    private Inmueble inmueble;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vigencia", nullable = false, length = 20)
    private ETipoVigencia tipoVigencia;

    @Column(name = "fecha_prevista")
    private LocalDate fechaPrevista;

    @Column(length = 255)
    private String motivo;

    @Column(name = "placa_vehiculo", length = 15)
    private String placaVehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_vehiculo_id", foreignKey = @ForeignKey(name = "fk_pre_registro_tipo_vehiculo"))
    private ValorCatalogo tipoVehiculo;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(nullable = false)
    private boolean usado;

    @Column(name = "fecha_hora_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHoraRegistro = LocalDateTime.now();

    public boolean estaVigente(LocalDate fecha) {
        if (!activo) {
            return false;
        }
        return tipoVigencia == ETipoVigencia.PERMANENTE
                || (!usado && fechaPrevista != null && fechaPrevista.equals(fecha));
    }

    public void marcarUsado() {
        this.usado = true;
    }

    public void cancelar() {
        this.activo = false;
    }

    public void desactivar() {
        this.activo = false;
    }
}
