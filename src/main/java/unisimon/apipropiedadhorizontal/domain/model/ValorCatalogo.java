package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;

/** Valor parametrizable (tipo de documento, de inmueble, de vehiculo, etc.) administrado por el Administrador. */
@Entity
@Table(name = "valor_catalogo",
        uniqueConstraints = @UniqueConstraint(name = "uk_valor_catalogo_tipo_nombre", columnNames = {"tipo", "nombre"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ValorCatalogo extends EntidadBase {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ETipoCatalogo tipo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    public void modificar(String nombre) {
        this.nombre = nombre;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void reactivar() {
        this.activo = true;
    }
}
