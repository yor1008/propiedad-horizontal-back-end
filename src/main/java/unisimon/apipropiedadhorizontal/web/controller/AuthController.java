package unisimon.apipropiedadhorizontal.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import unisimon.apipropiedadhorizontal.security.UsuarioActual;
import unisimon.apipropiedadhorizontal.security.anotaciones.Autenticado;
import unisimon.apipropiedadhorizontal.service.AutenticacionService;
import unisimon.apipropiedadhorizontal.service.ContrasenaService;
import unisimon.apipropiedadhorizontal.service.RestablecimientoContrasenaService;
import unisimon.apipropiedadhorizontal.web.dto.CambiarContrasenaRequest;
import unisimon.apipropiedadhorizontal.web.dto.LoginRequest;
import unisimon.apipropiedadhorizontal.web.dto.OlvideContrasenaRequest;
import unisimon.apipropiedadhorizontal.web.dto.PerfilResponse;
import unisimon.apipropiedadhorizontal.web.dto.RestablecerContrasenaRequest;
import unisimon.apipropiedadhorizontal.web.dto.TokenResponse;

/** CU-001 y CU-002. Login, olvide-contrasena y restablecer-contrasena son los unicos publicos (ver RutasPublicas). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticacionService autenticacion;
    private final ContrasenaService contrasenas;
    private final UsuarioActual usuarioActual;
    private final RestablecimientoContrasenaService restablecimiento;

    public AuthController(AutenticacionService autenticacion, ContrasenaService contrasenas,
                          UsuarioActual usuarioActual, RestablecimientoContrasenaService restablecimiento) {
        this.autenticacion = autenticacion;
        this.contrasenas = contrasenas;
        this.usuarioActual = usuarioActual;
        this.restablecimiento = restablecimiento;
    }

    /** Publico por configuracion de la cadena de filtros: no puede exigir un token que aun no existe. */
    @PostMapping("/login")
    public TokenResponse iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
        return TokenResponse.de(autenticacion.iniciarSesion(solicitud.correo(), solicitud.contrasena()));
    }

    /** Publico (RutasPublicas): quien olvido su clave no tiene token. Responde igual exista o no la cuenta. */
    @PostMapping("/olvide-contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void olvideContrasena(@Valid @RequestBody OlvideContrasenaRequest solicitud) {
        restablecimiento.solicitar(solicitud.correo());
    }

    /** Publico (RutasPublicas): la autorizacion es el token de un solo uso del enlace enviado por correo. */
    @PostMapping("/restablecer-contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecerContrasena(@Valid @RequestBody RestablecerContrasenaRequest solicitud) {
        restablecimiento.restablecer(solicitud.token(), solicitud.nueva());
    }

    @Autenticado
    @GetMapping("/me")
    public PerfilResponse perfil() {
        return PerfilResponse.de(usuarioActual);
    }

    @Autenticado
    @PostMapping("/cambiar-contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(@Valid @RequestBody CambiarContrasenaRequest solicitud) {
        contrasenas.cambiar(solicitud.actual(), solicitud.nueva());
    }
}
