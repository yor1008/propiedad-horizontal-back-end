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
 * {@code expiracionRestablecimiento} es la vigencia del enlace de restablecer contrasena (30 minutos si no se define).
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "el secreto JWT debe tener al menos 32 caracteres") String secret,
        @NotNull Duration expiracion,
        @NotBlank String emisor,
        Duration expiracionRestablecimiento) {

    public JwtProperties {
        if (expiracionRestablecimiento == null) {
            expiracionRestablecimiento = Duration.ofMinutes(30);
        }
    }

    /** Emisor distinto del de acceso: asi el JwtDecoder de la API rechaza un token de restablecimiento como acceso. */
    public String emisorRestablecimiento() {
        return emisor + "-restablecer";
    }
}
