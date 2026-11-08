package unisimon.apipropiedadhorizontal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import unisimon.apipropiedadhorizontal.web.error.ErrorApi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Responde 401/403 en JSON desde la cadena de filtros (antes de llegar a cualquier controlador). */
@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper json;

    public ManejadorErroresSeguridad(JsonMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escribir(response, ErrorApi.de(HttpStatus.UNAUTHORIZED,
                "Autenticacion requerida o token invalido/expirado", request.getRequestURI()));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        escribir(response, ErrorApi.de(HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta accion", request.getRequestURI()));
    }

    private void escribir(HttpServletResponse response, ErrorApi cuerpo) throws IOException {
        response.setStatus(cuerpo.estado());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json.writeValueAsString(cuerpo));
    }
}
