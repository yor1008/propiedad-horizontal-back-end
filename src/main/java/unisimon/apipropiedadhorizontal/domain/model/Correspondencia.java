package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

/** Paquete o documento recibido en porteria para un inmueble (y opcionalmente una persona). */
@Entity
@Table(name = "correspondencia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Correspondencia extends EntidadBase {

    /** Destinatario concreto (0..1); si es nulo va dirigida al inmueble. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", foreignKey = @ForeignKey(name = "fk_correspondencia_persona"))
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmueble_id", nullable = false, foreignKey = @ForeignKey(name = "fk_correspondencia_inmueble"))
    private Inmueble inmueble;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_correspondencia_tipo"))
    private ValorCatalogo tipo;

    @Column(length = 150)
    private String remitente;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "fecha_hora_recepcion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHoraRecepcion = LocalDateTime.now();

    @Column(name = "fecha_hora_entrega")
    private LocalDateTime fechaHoraEntrega;

    @Column(name = "nombre_recibe", length = 150)
    private String nombreRecibe;

    @Column(name = "documento_recibe", length = 30)
    private String documentoRecibe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false, foreignKey = @ForeignKey(name = "fk_correspondencia_estado"))
    private Estado estado;

    public void registrarEntrega(String nombreRecibe, String documentoRecibe) {
        this.nombreRecibe = nombreRecibe;
        this.documentoRecibe = documentoRecibe;
        this.fechaHoraEntrega = LocalDateTime.now();
    }

    public void cambiarEstado(Estado nuevo) {
        this.estado = nuevo;
    }

    public boolean estaPendiente() {
        return fechaHoraEntrega == null;
    }
}
