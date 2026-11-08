package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.EProcesoEstado;
import unisimon.apipropiedadhorizontal.domain.model.Estado;
import unisimon.apipropiedadhorizontal.domain.model.TransicionEstado;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.EstadoRepository;
import unisimon.apipropiedadhorizontal.repository.TransicionEstadoRepository;
import unisimon.apipropiedadhorizontal.service.GestorEstados;

@Service
@Transactional(readOnly = true)
public class GestorEstadosImpl implements GestorEstados {

    private final EstadoRepository estados;
    private final TransicionEstadoRepository transiciones;

    public GestorEstadosImpl(EstadoRepository estados, TransicionEstadoRepository transiciones) {
        this.estados = estados;
        this.transiciones = transiciones;
    }

    @Override
    public Estado inicial(EProcesoEstado proceso) {
        return estados.findByProcesoAndEsInicialTrue(proceso)
                .orElseThrow(() -> new ReglaNegocioException("No hay estado inicial configurado para " + proceso));
    }

    @Override
    public Estado porNombre(EProcesoEstado proceso, String nombre) {
        return estados.findByProcesoAndNombre(proceso, nombre)
                .orElseThrow(() -> new ReglaNegocioException("Estado '" + nombre + "' no configurado para " + proceso));
    }

    @Override
    public Estado transicionar(Estado actual, String nombreDestino, String motivo) {
        Estado destino = porNombre(actual.getProceso(), nombreDestino);
        TransicionEstado transicion = transiciones
                .findByEstadoOrigenIdAndEstadoDestinoId(actual.getId(), destino.getId())
                .orElseThrow(() -> new ReglaNegocioException(
                        "No se puede pasar de '" + actual.getNombre() + "' a '" + destino.getNombre() + "'"));
        if (transicion.isRequiereMotivo() && (motivo == null || motivo.isBlank())) {
            throw new ReglaNegocioException("Esta operacion exige indicar un motivo");
        }
        return destino;
    }
}
