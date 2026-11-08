package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;

import java.time.LocalDateTime;

/** Nunca expone el hash de la contrasena. */
public record CuentaResponse(Long id, String correo, ERolCuenta rol, boolean activa, Long personaId,
                             LocalDateTime fechaUltimoAcceso) {

    public static CuentaResponse de(CuentaUsuario c) {
        return new CuentaResponse(c.getId(), c.getCorreoAcceso(), c.getRol(), c.isActiva(),
                c.getPersona() == null ? null : c.getPersona().getId(), c.getFechaUltimoAcceso());
    }
}
