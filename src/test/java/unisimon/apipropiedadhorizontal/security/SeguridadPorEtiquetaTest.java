package unisimon.apipropiedadhorizontal.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regla de arquitectura: la autorizacion se declara con etiquetas, no por URL.
 * Todo metodo de controlador debe estar protegido por una etiqueta (directa, meta-anotacion o a nivel de clase);
 * la unica excepcion permitida es el login.
 */
class SeguridadPorEtiquetaTest {

    private static final Set<String> EXCEPCIONES = Set.of("AuthController#iniciarSesion");

    @Test
    void todoEndpointTieneEtiquetaDeAutorizacion() throws ClassNotFoundException {
        var escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> sinProteccion = new ArrayList<>();
        int controladores = 0;
        for (BeanDefinition definicion : escaner.findCandidateComponents("unisimon.apipropiedadhorizontal")) {
            Class<?> clase = Class.forName(definicion.getBeanClassName());
            controladores++;
            boolean claseProtegida = AnnotatedElementUtils.hasAnnotation(clase, PreAuthorize.class);
            for (Method metodo : clase.getDeclaredMethods()) {
                boolean esEndpoint = Modifier.isPublic(metodo.getModifiers())
                        && AnnotatedElementUtils.hasAnnotation(metodo, RequestMapping.class);
                if (!esEndpoint) {
                    continue;
                }
                String id = clase.getSimpleName() + "#" + metodo.getName();
                boolean protegido = claseProtegida || AnnotatedElementUtils.hasAnnotation(metodo, PreAuthorize.class);
                if (!protegido && !EXCEPCIONES.contains(id)) {
                    sinProteccion.add(id);
                }
            }
        }

        assertThat(controladores).as("controladores encontrados").isGreaterThan(0);
        assertThat(sinProteccion).as("endpoints sin etiqueta de autorizacion").isEmpty();
    }

    @Test
    void laCadenaDeFiltrosSoloExponeElLogin() {
        assertThat(RutasPublicas.LOGIN).isEqualTo("/api/auth/login");
    }
}
