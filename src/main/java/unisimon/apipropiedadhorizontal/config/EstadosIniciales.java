package unisimon.apipropiedadhorizontal.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import unisimon.apipropiedadhorizontal.domain.enums.EProcesoEstado;
import unisimon.apipropiedadhorizontal.domain.model.Estado;
import unisimon.apipropiedadhorizontal.domain.model.TransicionEstado;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.repository.EstadoRepository;
import unisimon.apipropiedadhorizontal.repository.ReservaRepository;
import unisimon.apipropiedadhorizontal.repository.TransicionEstadoRepository;

import static unisimon.apipropiedadhorizontal.domain.NombresEstado.*;
import static unisimon.apipropiedadhorizontal.domain.enums.EProcesoEstado.*;

/**
 * Siembra (de forma idempotente) los estados y transiciones de cada proceso. Solo crea lo que falta:
 * si el administrador ajusta los datos, un reinicio no los pisa.
 */
@Component
public class EstadosIniciales implements ApplicationRunner {

    private final EstadoRepository estados;
    private final TransicionEstadoRepository transiciones;
    private final ReservaRepository reservas;

    public EstadosIniciales(EstadoRepository estados, TransicionEstadoRepository transiciones,
                            ReservaRepository reservas) {
        this.estados = estados;
        this.transiciones = transiciones;
        this.reservas = reservas;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Estado pendiente = estado(CORRESPONDENCIA, CORRESPONDENCIA_PENDIENTE, true, false);
        Estado entregada = estado(CORRESPONDENCIA, CORRESPONDENCIA_ENTREGADA, false, true);
        transicion(pendiente, entregada, false);

        retirarEstadoReservaAntiguo();
        Estado pendienteReserva = estado(RESERVA, RESERVA_PENDIENTE, true, false);
        Estado aprobada = estado(RESERVA, RESERVA_APROBADA, false, false);
        Estado rechazada = estado(RESERVA, RESERVA_RECHAZADA, false, true);
        Estado cancelada = estado(RESERVA, RESERVA_CANCELADA, false, true);
        Estado finalizada = estado(RESERVA, RESERVA_FINALIZADA, false, true);
        transicion(pendienteReserva, aprobada, false);
        transicion(pendienteReserva, rechazada, true);
        transicion(pendienteReserva, cancelada, true);
        transicion(aprobada, cancelada, true);
        transicion(aprobada, finalizada, false);

        Estado programado = estado(MANTENIMIENTO, MANTENIMIENTO_PROGRAMADO, true, false);
        Estado enCurso = estado(MANTENIMIENTO, MANTENIMIENTO_EN_CURSO, false, false);
        Estado completado = estado(MANTENIMIENTO, MANTENIMIENTO_COMPLETADO, false, true);
        Estado cancelado = estado(MANTENIMIENTO, MANTENIMIENTO_CANCELADO, false, true);
        transicion(programado, enCurso, false);
        transicion(enCurso, completado, false);
        transicion(programado, cancelado, true);
        transicion(enCurso, cancelado, true);

        Estado radicada = estado(PQRS, PQRS_RADICADA, true, false);
        Estado enTramite = estado(PQRS, PQRS_EN_TRAMITE, false, false);
        Estado respondida = estado(PQRS, PQRS_RESPONDIDA, false, true);
        transicion(radicada, enTramite, false);
        transicion(radicada, respondida, false);
        transicion(enTramite, respondida, false);
    }

    /**
     * Version previa del flujo de reservas usaba "Activa" como inicial; dos iniciales romperian
     * {@code GestorEstados.inicial}. Se retira solo si ninguna reserva la usa.
     */
    private void retirarEstadoReservaAntiguo() {
        estados.findByProcesoAndNombre(RESERVA, "Activa").ifPresent(activa -> {
            if (!reservas.existsByEstadoId(activa.getId())) {
                transiciones.deleteByEstadoOrigenIdOrEstadoDestinoId(activa.getId(), activa.getId());
                estados.delete(activa);
            }
        });
    }

    private Estado estado(EProcesoEstado proceso, String nombre, boolean inicial, boolean fin) {
        return estados.findByProcesoAndNombre(proceso, nombre).orElseGet(() -> estados.save(
                Estado.builder().proceso(proceso).nombre(nombre).esInicial(inicial).esFinal(fin).build()));
    }

    private void transicion(Estado origen, Estado destino, boolean requiereMotivo) {
        if (transiciones.findByEstadoOrigenIdAndEstadoDestinoId(origen.getId(), destino.getId()).isEmpty()) {
            transiciones.save(TransicionEstado.builder()
                    .estadoOrigen(origen).estadoDestino(destino).requiereMotivo(requiereMotivo).build());
        }
    }
}
