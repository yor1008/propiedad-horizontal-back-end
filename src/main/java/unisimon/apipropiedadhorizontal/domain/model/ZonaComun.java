package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import unisimon.apipropiedadhorizontal.domain.enums.EDiaSemana;
import unisimon.apipropiedadhorizontal.domain.enums.EPeriodoLimite;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoUsoZona;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Salon, piscina, BBQ, etc. con sus reglas de reserva. Los horarios son parte de su ciclo de vida (composicion). */
@Entity
@Table(name = "zona_comun")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ZonaComun extends EntidadBase {

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "capacidad_maxima", nullable = false)
    private int capacidadMaxima;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_uso", nullable = false, length = 20)
    private ETipoUsoZona tipoUso;

    @Column(name = "requiere_aprobacion", nullable = false)
    private boolean requiereAprobacion;

    @Column(name = "duracion_maxima_minutos")
    private int duracionMaximaMinutos;

    @Column(name = "anticipacion_minima_horas")
    private int anticipacionMinimaHoras;

    @Column(name = "anticipacion_maxima_dias")
    private int anticipacionMaximaDias;

    @Column(name = "limite_cancelacion_horas")
    private int limiteCancelacionHoras;

    @Column(name = "max_reservas_por_inmueble")
    private int maxReservasPorInmueble;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodo_limite", length = 10)
    private EPeriodoLimite periodoLimite;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;

    @Column(name = "motivo_desactivacion", length = 255)
    private String motivoDesactivacion;

    @OneToMany(mappedBy = "zonaComun", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HorarioDisponibilidad> horarios = new ArrayList<>();

    public void configurarReglas(int duracionMax, int antMin, int antMax, int limiteCancel, int maxReservas,
                                 EPeriodoLimite periodo) {
        this.duracionMaximaMinutos = duracionMax;
        this.anticipacionMinimaHoras = antMin;
        this.anticipacionMaximaDias = antMax;
        this.limiteCancelacionHoras = limiteCancel;
        this.maxReservasPorInmueble = maxReservas;
        this.periodoLimite = periodo;
    }

    public void modificarDatos(String nombre, String descripcion, int capacidad, ETipoUsoZona tipoUso,
                               boolean requiereAprobacion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.capacidadMaxima = capacidad;
        this.tipoUso = tipoUso;
        this.requiereAprobacion = requiereAprobacion;
    }

    /** Quita todos los horarios (orphanRemoval los borra); el servicio agrega los nuevos con agregarHorario. */
    public void limpiarHorarios() {
        horarios.clear();
    }

    public void agregarHorario(EDiaSemana dia, java.time.LocalTime inicio, java.time.LocalTime fin) {
        horarios.add(HorarioDisponibilidad.builder().zonaComun(this).diaSemana(dia)
                .horaInicio(inicio).horaFin(fin).build());
    }

    public List<HorarioDisponibilidad> consultarDisponibilidad(LocalDate fecha) {
        EDiaSemana dia = EDiaSemana.desde(fecha.getDayOfWeek());
        return horarios.stream().filter(h -> h.getDiaSemana() == dia).toList();
    }

    /**
     * Valida las reglas propias de la zona (activa, horario, capacidad, duracion y anticipacion).
     * Los limites por inmueble y traslapes requieren consultar otras reservas y los resuelve el servicio.
     */
    public boolean validarReserva(Reserva reserva) {
        if (!activa) {
            return false;
        }
        boolean enHorario = horarios.stream()
                .anyMatch(h -> h.contiene(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin()));
        if (!enHorario || reserva.getNumeroAsistentes() > capacidadMaxima) {
            return false;
        }
        long minutos = Duration.between(reserva.getHoraInicio(), reserva.getHoraFin()).toMinutes();
        if (duracionMaximaMinutos > 0 && minutos > duracionMaximaMinutos) {
            return false;
        }
        LocalDateTime inicio = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());
        LocalDateTime ahora = LocalDateTime.now();
        return !inicio.isBefore(ahora.plusHours(anticipacionMinimaHoras))
                && (anticipacionMaximaDias <= 0 || !inicio.isAfter(ahora.plusDays(anticipacionMaximaDias)));
    }

    public void desactivar(String motivo) {
        this.activa = false;
        this.motivoDesactivacion = motivo;
    }

    public void reactivar() {
        this.activa = true;
        this.motivoDesactivacion = null;
    }
}
