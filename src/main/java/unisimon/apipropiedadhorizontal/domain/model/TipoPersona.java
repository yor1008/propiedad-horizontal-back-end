package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Clasifica a una {@link Persona} vinculada (Propietario, Residente...) segun los permisos que implica. */
@Entity
@Table(name = "tipo_persona")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class TipoPersona extends EntidadBase {

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "es_propietario", nullable = false)
    private boolean esPropietario;

    @Column(name = "es_residente", nullable = false)
    private boolean esResidente;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    public void modificar(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void reactivar() {
        this.activo = true;
    }
}
