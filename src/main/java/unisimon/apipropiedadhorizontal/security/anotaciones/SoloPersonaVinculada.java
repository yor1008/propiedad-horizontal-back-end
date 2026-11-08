package unisimon.apipropiedadhorizontal.security.anotaciones;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Propietarios y residentes (CU-003, CU-004, CU-023, CU-029, CU-030). */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('PERSONA_VINCULADA')")
public @interface SoloPersonaVinculada {
}
