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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/** Persona vinculada (propietario, residente...) a un inmueble del conjunto. */
@Entity
@Table(name = "persona",
        uniqueConstraints = @UniqueConstraint(name = "uk_persona_documento",
                columnNames = {"tipo_documento_id", "numero_documento"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Persona extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_persona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_persona_tipo_persona"))
    private TipoPersona tipoPersona;

    /** Inmueble al que esta vinculada (0..1: puede no tener vinculo vigente). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inmueble_id", foreignKey = @ForeignKey(name = "fk_persona_inmueble"))
    private Inmueble inmueble;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_documento_id", nullable = false, foreignKey = @ForeignKey(name = "fk_persona_tipo_documento"))
    private ValorCatalogo tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String correo;

    @Column(name = "fecha_inicio_vinculo")
    private LocalDate fechaInicioVinculo;

    @Column(name = "fecha_fin_vinculo")
    private LocalDate fechaFinVinculo;

    @Column(name = "motivo_fin_vinculo", length = 255)
    private String motivoFinVinculo;

    @Column(name = "autoriza_tratamiento_datos", nullable = false)
    private boolean autorizaTratamientoDatos;

    @Column(name = "fecha_autorizacion_datos")
    private LocalDate fechaAutorizacionDatos;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;

    @Column(name = "motivo_desactivacion", length = 255)
    private String motivoDesactivacion;

    /** Corrige los datos personales; el documento no cambia porque identifica a la persona. */
    public void actualizarDatos(String nombres, String apellidos, String telefono, String correo) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        actualizarContacto(telefono, correo);
    }

    public void actualizarContacto(String telefono, String correo) {
        this.telefono = telefono;
        this.correo = correo;
    }

    public void vincularInmueble(Inmueble inmueble, TipoPersona tipo, LocalDate fechaInicio) {
        this.inmueble = inmueble;
        this.tipoPersona = tipo;
        this.fechaInicioVinculo = fechaInicio;
        this.fechaFinVinculo = null;
        this.motivoFinVinculo = null;
    }

    public void finalizarVinculo(LocalDate fechaFin, String motivo) {
        this.fechaFinVinculo = fechaFin;
        this.motivoFinVinculo = motivo;
    }

    public boolean vinculoEstaVigente() {
        LocalDate hoy = LocalDate.now();
        return activa
                && inmueble != null
                && fechaInicioVinculo != null
                && !fechaInicioVinculo.isAfter(hoy)
                && (fechaFinVinculo == null || !fechaFinVinculo.isBefore(hoy));
    }

    public boolean esPropietarioDe(Inmueble otro) {
        return vinculoEstaVigente() && inmueble.equals(otro) && tipoPersona.isEsPropietario();
    }

    public boolean esResidenteDe(Inmueble otro) {
        return vinculoEstaVigente() && inmueble.equals(otro) && tipoPersona.isEsResidente();
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
