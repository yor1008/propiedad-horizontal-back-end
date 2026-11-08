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
import unisimon.apipropiedadhorizontal.domain.enums.EResultadoContacto;

import java.time.LocalDateTime;

/** Llegada de un visitante al conjunto, con o sin pre-registro, y su ciclo de ingreso/salida. */
@Entity
@Table(name = "registro_visita")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class RegistroVisita extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_registro_id", foreignKey = @ForeignKey(name = "fk_registro_visita_pre_registro"))
    private PreRegistro preRegistro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitante_id", nullable = false, foreignKey = @ForeignKey(name = "fk_registro_visita_visitante"))
    private Visitante visitante;

    /** Inmueble visitado. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmueble_id", nullable = false, foreignKey = @ForeignKey(name = "fk_registro_visita_inmueble"))
    private Inmueble inmueble;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "residente_contactado_id", foreignKey = @ForeignKey(name = "fk_registro_visita_residente"))
    private Persona residenteContactado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_asignado_id", foreignKey = @ForeignKey(name = "fk_registro_visita_parqueadero"))
    private Inmueble parqueaderoAsignado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_vehiculo_id", foreignKey = @ForeignKey(name = "fk_registro_visita_tipo_vehiculo"))
    private ValorCatalogo tipoVehiculo;

    @Enumerated(EnumType.STRING)
    @Column(name = "respuesta_residente", length = 20)
    private EResultadoContacto respuestaResidente;

    @Column(name = "ingreso_autorizado", nullable = false)
    private boolean ingresoAutorizado;

    @Column(name = "motivo_negacion", length = 255)
    private String motivoNegacion;

    @Column(name = "fecha_hora_llegada", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHoraLlegada = LocalDateTime.now();

    @Column(name = "fecha_hora_entrada")
    private LocalDateTime fechaHoraEntrada;

    @Column(name = "fecha_hora_salida")
    private LocalDateTime fechaHoraSalida;

    public void autorizarIngreso() {
        this.ingresoAutorizado = true;
        this.motivoNegacion = null;
        this.fechaHoraEntrada = LocalDateTime.now();
    }

    public void negarIngreso(String motivo) {
        this.ingresoAutorizado = false;
        this.motivoNegacion = motivo;
    }

    public void registrarContactoResidente(Persona residente, EResultadoContacto respuesta) {
        this.residenteContactado = residente;
        this.respuestaResidente = respuesta;
    }

    public void asignarParqueadero(Inmueble parqueadero) {
        this.parqueaderoAsignado = parqueadero;
    }

    public void registrarSalida() {
        if (!estaDentro()) {
            throw new IllegalStateException("El visitante no se encuentra dentro del conjunto");
        }
        this.fechaHoraSalida = LocalDateTime.now();
    }

    public boolean estaDentro() {
        return ingresoAutorizado && fechaHoraEntrada != null && fechaHoraSalida == null;
    }
}
