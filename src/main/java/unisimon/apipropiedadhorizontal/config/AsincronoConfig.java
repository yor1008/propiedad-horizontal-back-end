package unisimon.apipropiedadhorizontal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita {@code @Async} (lo usa el envio de correos para no bloquear la peticion). */
@Configuration
@EnableAsync
public class AsincronoConfig {
}
