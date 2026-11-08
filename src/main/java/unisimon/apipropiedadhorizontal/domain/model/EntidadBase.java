package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.Hibernate;

import java.util.Objects;

/**
 * Superclase de todas las entidades: aporta el identificador {@code Long} del diagrama de clases
 * y una igualdad basada en identidad de persistencia (segura con proxies lazy de Hibernate).
 */
@MappedSuperclass
@Getter
@NoArgsConstructor
@SuperBuilder
public abstract class EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        EntidadBase otra = (EntidadBase) o;
        // getId() y no el campo: en un proxy lazy de Hibernate el campo es null (equals es final, corre en el proxy).
        return getId() != null && Objects.equals(getId(), otra.getId());
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
