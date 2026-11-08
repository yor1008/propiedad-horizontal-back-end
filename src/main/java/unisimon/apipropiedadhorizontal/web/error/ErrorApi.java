package unisimon.apipropiedadhorizontal.web.error;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/** Cuerpo unico de error de la API (tanto para excepciones de controlador como para 401/403 de seguridad). */
public record ErrorApi(int estado, String error, String mensaje, String ruta, String fecha, List<String> detalles) {

    public static ErrorApi de(HttpStatus estado, String mensaje, String ruta) {
        return de(estado, mensaje, ruta, List.of());
    }

    public static ErrorApi de(HttpStatus estado, String mensaje, String ruta, List<String> detalles) {
        return new ErrorApi(estado.value(), estado.getReasonPhrase(), mensaje, ruta, Instant.now().toString(), detalles);
    }
}
