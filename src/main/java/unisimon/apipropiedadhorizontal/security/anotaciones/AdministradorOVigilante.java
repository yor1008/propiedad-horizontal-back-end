package unisimon.apipropiedadhorizontal.security.anotaciones;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Operaciones de porteria compartidas (p. ej. CU-021 historial de accesos). */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'VIGILANTE')")
public @interface AdministradorOVigilante {
}
