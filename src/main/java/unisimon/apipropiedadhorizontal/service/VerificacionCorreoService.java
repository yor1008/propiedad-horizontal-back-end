package unisimon.apipropiedadhorizontal.service;

/** Comprueba que una persona recibe los correos de la direccion que se le registra (codigo de un solo uso). */
public interface VerificacionCorreoService {

    /** Genera un codigo de 6 digitos, lo guarda (solo su huella) y lo envia al correo. Reemplaza el codigo anterior. */
    void enviarCodigo(String correo);

    /**
     * Valida el codigo y lo consume. Codigo ausente, equivocado, vencido o con demasiados intentos lanza
     * {@code ReglaNegocioException}. Los intentos fallidos se conservan (la transaccion no se revierte por esa excepcion).
     */
    void confirmar(String correo, String codigo);
}
