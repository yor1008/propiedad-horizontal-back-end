package unisimon.apipropiedadhorizontal.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;

import java.util.List;
import java.util.Optional;

@Component
public class UsuarioActualJwt implements UsuarioActual {

    @Override
    public Long cuentaId() {
        return Long.valueOf(jwt().getSubject());
    }

    @Override
    public String correo() {
        return jwt().getClaimAsString(JwtTokenService.CLAIM_CORREO);
    }

    @Override
    public ERolCuenta rol() {
        return ERolCuenta.valueOf(jwt().getClaimAsString(JwtTokenService.CLAIM_ROL));
    }

    @Override
    public Optional<Long> personaId() {
        Object valor = jwt().getClaim(JwtTokenService.CLAIM_PERSONA_ID);
        return valor instanceof Number n ? Optional.of(n.longValue()) : Optional.empty();
    }

    @Override
    public List<String> autoridades() {
        List<String> valor = jwt().getClaimAsStringList(JwtTokenService.CLAIM_AUTORIDADES);
        return valor == null ? List.of() : valor;
    }

    private Jwt jwt() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new IllegalStateException("No hay un JWT autenticado en el contexto de seguridad");
    }
}
