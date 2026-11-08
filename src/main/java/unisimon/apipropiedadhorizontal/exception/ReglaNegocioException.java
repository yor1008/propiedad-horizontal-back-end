package unisimon.apipropiedadhorizontal.exception;

/** Se violo una regla del dominio (se traduce a HTTP 422). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
