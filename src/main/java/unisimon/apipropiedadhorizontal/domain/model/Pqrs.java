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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Peticion, queja, reclamo o sugerencia radicada por una persona vinculada. */
@Entity
@Table(name = "pqrs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Pqrs extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pqrs_persona"))
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmueble_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pqrs_inmueble"))
    private Inmueble inmueble;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pqrs_tipo"))
    private ValorCatalogo tipo;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    /** Referencia (nombre o ruta) del archivo adjunto; el archivo en si no se guarda en la API. */
    @Column(length = 500)
    private String adjunto;

    @Column(name = "fecha_hora_radicacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHoraRadicacion = LocalDateTime.now();

    @Column(length = 2000)
    private String respuesta;

    @Column(name = "fecha_hora_respuesta")
    private LocalDateTime fechaHoraRespuesta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pqrs_estado"))
    private Estado estado;

    public void cambiarEstado(Estado nuevo) {
        this.estado = nuevo;
    }

    public void responder(String respuesta) {
        this.respuesta = respuesta;
        this.fechaHoraRespuesta = LocalDateTime.now();
    }

    /** Sin respuesta y con mas dias habiles (lunes a viernes, sin festivos) transcurridos que el plazo. */
    public boolean estaVencida(int plazoDiasHabiles) {
        if (fechaHoraRespuesta != null) {
            return false;
        }
        LocalDate hoy = LocalDate.now();
        int habiles = 0;
        for (LocalDate dia = fechaHoraRadicacion.toLocalDate().plusDays(1); !dia.isAfter(hoy); dia = dia.plusDays(1)) {
            DayOfWeek d = dia.getDayOfWeek();
            if (d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY) {
                habiles++;
            }
        }
        return habiles > plazoDiasHabiles;
    }
}
