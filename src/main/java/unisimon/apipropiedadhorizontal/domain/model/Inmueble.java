package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.ColumnDefault;
import unisimon.apipropiedadhorizontal.domain.enums.ECategoriaInmueble;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Unidad inmobiliaria: apartamento, local, parqueadero, etc. (el tipo lo define un {@link ValorCatalogo}).
 * Un parqueadero es un Inmueble cuyo {@code inmuebleAsociado} apunta al inmueble al que pertenece.
 */
@Entity
@Table(name = "inmueble",
        uniqueConstraints = @UniqueConstraint(name = "uk_inmueble_torre_numero", columnNames = {"torre_bloque", "numero"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Inmueble extends EntidadBase {

    @Column(name = "torre_bloque", nullable = false, length = 30)
    private String torreBloque;

    @Column(nullable = false, length = 30)
    private String numero;

    @Column(nullable = false)
    private int piso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inmueble_tipo"))
    private ValorCatalogo tipo;

    /** Inmueble principal al que pertenece este parqueadero (0..1). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inmueble_asociado_id", foreignKey = @ForeignKey(name = "fk_inmueble_asociado"))
    private Inmueble inmuebleAsociado;

    /** Unidad privada o parqueadero (los parqueaderos no se vinculan a personas). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'UNIDAD_PRIVADA'")
    @Builder.Default
    private ECategoriaInmueble categoria = ECategoriaInmueble.UNIDAD_PRIVADA;

    @Column(nullable = false)
    private boolean ocupado;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "motivo_desactivacion", length = 255)
    private String motivoDesactivacion;

    /** Parqueaderos asignados a este inmueble: lado inverso de {@code inmuebleAsociado} (no agrega columnas). */
    @OneToMany(mappedBy = "inmuebleAsociado")
    @Getter(AccessLevel.NONE)
    @Builder.Default
    private List<Inmueble> parqueaderos = new ArrayList<>();

    /**
     * Personas cuyo vinculo (vigente o pasado) apunta a este inmueble: lado inverso de {@code Persona.inmueble}.
     * Se lee de la base al primer acceso; un servicio que acabe de cambiar un vinculo en la misma transaccion debe
     * consultar el repositorio (ver {@code OcupacionInmuebles}).
     */
    @OneToMany(mappedBy = "inmueble")
    @Getter(AccessLevel.NONE)
    @Builder.Default
    private List<Persona> personas = new ArrayList<>();

    /** Fija los datos del inmueble al darlo de alta; la categoria queda como se construyo (unidad privada por defecto). */
    public void registrar(String torre, String numero, int piso, ValorCatalogo tipo) {
        this.torreBloque = torre;
        this.numero = numero;
        this.piso = piso;
        this.tipo = tipo;
        this.ocupado = false;
        this.activo = true;
        this.motivoDesactivacion = null;
    }

    public void modificarDatos(String torreBloque, String numero, int piso, ValorCatalogo tipo) {
        this.torreBloque = torreBloque;
        this.numero = numero;
        this.piso = piso;
        this.tipo = tipo;
    }

    public void asignarParqueadero(Inmueble parqueadero) {
        parqueadero.inmuebleAsociado = this;
        parqueaderos.add(parqueadero);
    }

    public void retirarParqueadero(Inmueble parqueadero) {
        if (this.equals(parqueadero.inmuebleAsociado)) {
            parqueadero.inmuebleAsociado = null;
            parqueaderos.remove(parqueadero);
        }
    }

    /** Parqueaderos asignados a este inmueble (copia de solo lectura). */
    public List<Inmueble> obtenerParqueaderos() {
        return List.copyOf(parqueaderos);
    }

    /** Personas con vinculo vigente y tipo de persona residente. */
    public List<Persona> obtenerResidentesVigentes() {
        return personas.stream().filter(p -> p.esResidenteDe(this)).toList();
    }

    /** Primer propietario con vinculo vigente, o {@code null} si no hay. */
    public Persona obtenerPropietarioVigente() {
        return personas.stream().filter(p -> p.esPropietarioDe(this)).findFirst().orElse(null);
    }

    public void marcarOcupado(boolean ocupado) {
        this.ocupado = ocupado;
    }

    public void desactivar(String motivo) {
        this.activo = false;
        this.motivoDesactivacion = motivo;
    }

    public void reactivar() {
        this.activo = true;
        this.motivoDesactivacion = null;
    }
}
