package unisimon.apipropiedadhorizontal.exception;

/** El recurso ya existe o el estado actual impide la operacion (se traduce a HTTP 409). */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
