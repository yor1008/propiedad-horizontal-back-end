package unisimon.apipropiedadhorizontal.exception;

/** El recurso solicitado no existe (se traduce a HTTP 404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " con id " + id + " no existe");
    }
}
