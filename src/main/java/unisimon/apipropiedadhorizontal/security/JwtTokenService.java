package unisimon.apipropiedadhorizontal.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Emite JWT firmados con HS256. Sin estado: nada del token se guarda en el servidor. */
@Service
public class JwtTokenService implements TokenService {

    public static final String CLAIM_CORREO = "correo";
    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_PERSONA_ID = "personaId";
    public static final String CLAIM_AUTORIDADES = "autoridades";

    private final JwtEncoder encoder;
    private final JwtProperties propiedades;
    private final ResolutorAutoridades resolutor;
    private final Clock reloj;

    public JwtTokenService(JwtEncoder encoder, JwtProperties propiedades, ResolutorAutoridades resolutor, Clock reloj) {
        this.encoder = encoder;
        this.propiedades = propiedades;
        this.resolutor = resolutor;
        this.reloj = reloj;
    }

    @Override
    public TokenEmitido emitir(CuentaUsuario cuenta) {
        Instant ahora = reloj.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(propiedades.emisor())
                .subject(String.valueOf(cuenta.getId()))
                .id(UUID.randomUUID().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(propiedades.expiracion()))
                .claim(CLAIM_CORREO, cuenta.getCorreoAcceso())
                .claim(CLAIM_ROL, cuenta.getRol().name())
                .claim(CLAIM_AUTORIDADES, resolutor.resolver(cuenta));
        if (cuenta.getPersona() != null) {
            claims.claim(CLAIM_PERSONA_ID, cuenta.getPersona().getId());
        }

        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(cabecera, claims.build())).getTokenValue();
        return new TokenEmitido(token, TokenEmitido.TIPO_BEARER, propiedades.expiracion().toSeconds());
    }
}
