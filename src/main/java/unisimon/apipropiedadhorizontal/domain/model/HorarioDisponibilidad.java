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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import unisimon.apipropiedadhorizontal.domain.enums.EDiaSemana;

import java.time.LocalDate;
import java.time.LocalTime;

/** Franja horaria en la que una {@link ZonaComun} se puede reservar un dia de la semana. */
@Entity
@Table(name = "horario_disponibilidad")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class HorarioDisponibilidad extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zona_comun_id", nullable = false, foreignKey = @ForeignKey(name = "fk_horario_zona_comun"))
    private ZonaComun zonaComun;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 15)
    private EDiaSemana diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    public void modificarHorario(LocalTime inicio, LocalTime fin) {
        this.horaInicio = inicio;
        this.horaFin = fin;
    }

    /** Verdadero si [inicio, fin] cae completamente dentro de esta franja y coincide el dia de la semana. */
    public boolean contiene(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        return EDiaSemana.desde(fecha.getDayOfWeek()) == diaSemana
                && !inicio.isBefore(horaInicio)
                && !fin.isAfter(horaFin);
    }
}
