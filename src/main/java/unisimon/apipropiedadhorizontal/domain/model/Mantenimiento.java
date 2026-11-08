package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/** Mantenimiento programado de una zona comun o de un elemento del conjunto. */
@Entity
@Table(name = "mantenimiento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Mantenimiento extends EntidadBase {

    /** Zona afectada (0..1); nulo si el mantenimiento no bloquea ninguna zona reservable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_comun_id", foreignKey = @ForeignKey(name = "fk_mantenimiento_zona_comun"))
    private ZonaComun zonaComun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mantenimiento_tipo"))
    private ValorCatalogo tipo;

    @Column(name = "elemento_area", nullable = false, length = 150)
    private String elementoArea;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin")
    private LocalDateTime fechaHoraFin;

    @Column(length = 150)
    private String responsable;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mantenimiento_estado"))
    private Estado estado;

    public void modificarDatos(String descripcion, LocalDateTime inicio, LocalDateTime fin, String responsable) {
        this.descripcion = descripcion;
        this.fechaHoraInicio = inicio;
        this.fechaHoraFin = fin;
        this.responsable = responsable;
    }

    public void cambiarEstado(Estado nuevo) {
        this.estado = nuevo;
    }

    public void finalizar(String observaciones) {
        this.observaciones = observaciones;
    }

    public void cancelar(String motivo) {
        this.motivoCancelacion = motivo;
    }

    /** Traslape con [inicio, fin); una fecha de fin nula se interpreta como periodo abierto. */
    public boolean afectaPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        return fechaHoraInicio.isBefore(fin) && (fechaHoraFin == null || fechaHoraFin.isAfter(inicio));
    }
}
