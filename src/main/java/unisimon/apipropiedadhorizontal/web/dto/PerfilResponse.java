package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.security.UsuarioActual;

import java.util.List;

/** Identidad tal como la afirma el token (no consulta la base de datos). */
public record PerfilResponse(Long cuentaId, String correo, ERolCuenta rol, Long personaId, List<String> autoridades) {

    public static PerfilResponse de(UsuarioActual usuario) {
        return new PerfilResponse(usuario.cuentaId(), usuario.correo(), usuario.rol(),
                usuario.personaId().orElse(null), usuario.autoridades());
    }
}
