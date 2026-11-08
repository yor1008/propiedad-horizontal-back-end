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

/** Vehiculo registrado de un inmueble; la {@link Persona} es la responsable. */
@Entity
@Table(name = "vehiculo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Vehiculo extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vehiculo_persona"))
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmueble_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vehiculo_inmueble"))
    private Inmueble inmueble;

    @Column(nullable = false, unique = true, length = 15)
    private String placa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vehiculo_tipo"))
    private ValorCatalogo tipo;

    @Column(length = 60)
    private String marca;

    @Column(length = 40)
    private String color;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "motivo_desactivacion", length = 255)
    private String motivoDesactivacion;

    public void modificarDatos(ValorCatalogo tipo, String marca, String color) {
        this.tipo = tipo;
        this.marca = marca;
        this.color = color;
    }

    public void desactivar(String motivo) {
        this.activo = false;
        this.motivoDesactivacion = motivo;
    }
}
