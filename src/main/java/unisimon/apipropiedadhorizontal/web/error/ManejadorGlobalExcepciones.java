package unisimon.apipropiedadhorizontal.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;

import java.util.List;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    /** Mensaje unico para credenciales malas, cuenta inexistente o inactiva: evita enumerar cuentas. */
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorApi> autenticacion(HttpServletRequest req) {
        return responder(HttpStatus.UNAUTHORIZED, "Credenciales invalidas", req);
    }

    /** Cubre tambien AuthorizationDeniedException, lanzada por las anotaciones @PreAuthorize. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorApi> accesoDenegado(HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta accion", req);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ErrorApi> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ResponseEntity<ErrorApi> reglaNegocio(ReglaNegocioException ex, HttpServletRequest req) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), req);
    }

    @ExceptionHandler(ConflictoException.class)
    ResponseEntity<ErrorApi> conflicto(ConflictoException ex, HttpServletRequest req) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    /** Red de seguridad ante carreras sobre restricciones unicas; no filtra detalles de la base de datos. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorApi> integridad(HttpServletRequest req) {
        return responder(HttpStatus.CONFLICT, "La operacion viola una restriccion de integridad de datos", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .toList();
        ErrorApi cuerpo = ErrorApi.de(HttpStatus.BAD_REQUEST, "Solicitud invalida", req.getRequestURI(), detalles);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorApi> solicitudMalFormada(HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "Solicitud mal formada o con valores no reconocidos", req);
    }

    private ResponseEntity<ErrorApi> responder(HttpStatus estado, String mensaje, HttpServletRequest req) {
        return ResponseEntity.status(estado).body(ErrorApi.de(estado, mensaje, req.getRequestURI()));
    }
}
