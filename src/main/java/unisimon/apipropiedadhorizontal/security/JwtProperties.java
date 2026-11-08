package unisimon.apipropiedadhorizontal.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuracion del JWT ({@code app.security.jwt.*}).
 * El secreto HS256 debe tener al menos 32 caracteres y proviene del entorno ({@code JWT_SECRET}); nunca se versiona.
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "el secreto JWT debe tener al menos 32 caracteres") String secret,
        @NotNull Duration expiracion,
        @NotBlank String emisor) {
}
