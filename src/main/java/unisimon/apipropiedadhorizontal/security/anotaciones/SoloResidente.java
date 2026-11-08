package unisimon.apipropiedadhorizontal.security.anotaciones;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Persona vinculada cuyo TipoPersona es residente con vinculo vigente (CU-015, CU-025, CU-026). */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('RESIDENTE')")
public @interface SoloResidente {
}
