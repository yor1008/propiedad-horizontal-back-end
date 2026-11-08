package unisimon.apipropiedadhorizontal.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class JwtTokenRestablecimientoService implements TokenRestablecimientoService {

    static final String CLAIM_HUELLA = "huella";
    private static final String MENSAJE_INVALIDO = "El enlace de restablecimiento no es valido o ya expiro";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties propiedades;
    private final Clock reloj;

    public JwtTokenRestablecimientoService(SecretKey claveJwt, JwtProperties propiedades, Clock reloj) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
        // Firma, expiracion y emisor de restablecimiento: un token de acceso no pasa esta validacion.
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(propiedades.emisorRestablecimiento()));
        this.decoder = decodificador;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    public String emitir(CuentaUsuario cuenta) {
        Instant ahora = reloj.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.emisorRestablecimiento())
                .subject(String.valueOf(cuenta.getId()))
                .id(UUID.randomUUID().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(propiedades.expiracionRestablecimiento()))
                .claim(CLAIM_HUELLA, huellaDe(cuenta))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Override
    public Reclamos validar(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            String huella = jwt.getClaimAsString(CLAIM_HUELLA);
            if (huella == null || jwt.getSubject() == null) {
                throw new ReglaNegocioException(MENSAJE_INVALIDO);
            }
            return new Reclamos(Long.valueOf(jwt.getSubject()), huella);
        } catch (JwtException | NumberFormatException e) {
            throw new ReglaNegocioException(MENSAJE_INVALIDO);
        }
    }

    @Override
    public String huellaDe(CuentaUsuario cuenta) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(cuenta.getHashContrasena().getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
