package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Reserva de una {@link ZonaComun} hecha por una persona en nombre de su inmueble. */
@Entity
@Table(name = "reserva", indexes = @Index(name = "ix_reserva_zona_fecha", columnList = "zona_comun_id, fecha"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Reserva extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reserva_persona"))
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmueble_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reserva_inmueble"))
    private Inmueble inmueble;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zona_comun_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reserva_zona_comun"))
    private ZonaComun zonaComun;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "numero_asistentes", nullable = false)
    private int numeroAsistentes;

    @Column(length = 255)
    private String motivo;

    @Column(name = "condiciones_uso", length = 500)
    private String condicionesUso;

    @Column(name = "motivo_rechazo_cancelacion", length = 255)
    private String motivoRechazoCancelacion;

    @Column(name = "fecha_hora_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHoraRegistro = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reserva_estado"))
    private Estado estado;

    public void cambiarEstado(Estado nuevo) {
        this.estado = nuevo;
    }

    public void aprobar(String condiciones) {
        this.condicionesUso = condiciones;
    }

    public void rechazar(String motivo) {
        this.motivoRechazoCancelacion = motivo;
    }

    public void cancelar(String motivo) {
        this.motivoRechazoCancelacion = motivo;
    }

    /** El residente solo cancela hasta {@code limiteCancelacionHoras} antes del inicio de la reserva. */
    public boolean puedeCancelarResidente() {
        LocalDateTime inicio = LocalDateTime.of(fecha, horaInicio);
        return LocalDateTime.now().isBefore(inicio.minusHours(zonaComun.getLimiteCancelacionHoras()));
    }

    public boolean seTraslapaCon(Reserva otra) {
        return zonaComun.equals(otra.zonaComun)
                && fecha.equals(otra.fecha)
                && horaInicio.isBefore(otra.horaFin)
                && otra.horaInicio.isBefore(horaFin);
    }
}
