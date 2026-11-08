package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.Estado;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;

/**
 * Regla comun del borrado fisico: solo procede si el registro no tiene historia (misma politica que personas e
 * inmuebles). Quien tenga historia responde 409 y se usa la baja logica (desactivar, cancelar o anular).
 */
final class Borrado {

    private Borrado() {
    }

    /**
     * Borra y fuerza el flush para que una referencia que apunte al registro (clave foranea) falle aqui y se traduzca
     * a 409, en vez de explotar al confirmar la transaccion. Debe llamarse dentro de una transaccion.
     */
    static <T> void fisico(JpaRepository<T, Long> repositorio, T entidad, String mensajeSiTieneHistoria) {
        try {
            repositorio.delete(entidad);
            repositorio.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoException(mensajeSiTieneHistoria);
        }
    }

    /** Un registro con maquina de estados solo se borra si nunca avanzo de su estado inicial. */
    static void exigirEstadoInicial(Estado estado, String registro) {
        if (!estado.isEsInicial()) {
            throw new ConflictoException(registro + " ya tuvo movimiento (estado '" + estado.getNombre()
                    + "') y no se puede borrar; use su accion de cancelar o anular");
        }
    }
}
