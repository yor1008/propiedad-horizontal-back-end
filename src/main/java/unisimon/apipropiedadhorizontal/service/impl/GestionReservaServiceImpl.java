package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.NombresEstado;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.model.Reserva;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.repository.ReservaRepository;
import unisimon.apipropiedadhorizontal.service.GestionReservaService;
import unisimon.apipropiedadhorizontal.service.GestorEstados;
import unisimon.apipropiedadhorizontal.service.Notificador;
import unisimon.apipropiedadhorizontal.web.dto.ReservaResponse;

import java.util.List;

@Service
@Transactional
public class GestionReservaServiceImpl implements GestionReservaService {

    private final ReservaRepository reservas;
    private final GestorEstados estados;
    private final Notificador notificador;

    public GestionReservaServiceImpl(ReservaRepository reservas, GestorEstados estados, Notificador notificador) {
        this.reservas = reservas;
        this.estados = estados;
        this.notificador = notificador;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listar() {
        return reservas.findAllByOrderByFechaDescHoraInicioDesc().stream().map(ReservaResponse::de).toList();
    }

    @Override
    public ReservaResponse aprobar(Long id, String condiciones) {
        Reserva r = buscar(id);
        r.cambiarEstado(estados.transicionar(r.getEstado(), NombresEstado.RESERVA_APROBADA, null));
        r.aprobar(condiciones == null || condiciones.isBlank() ? null : condiciones.trim());
        avisar(r, "fue aprobada");
        return ReservaResponse.de(r);
    }

    @Override
    public ReservaResponse rechazar(Long id, String motivo) {
        Reserva r = buscar(id);
        r.cambiarEstado(estados.transicionar(r.getEstado(), NombresEstado.RESERVA_RECHAZADA, motivo));
        r.rechazar(motivo.trim());
        avisar(r, "fue rechazada. Motivo: " + motivo.trim());
        return ReservaResponse.de(r);
    }

    @Override
    public ReservaResponse anular(Long id, String motivo) {
        Reserva r = buscar(id);
        r.cambiarEstado(estados.transicionar(r.getEstado(), NombresEstado.RESERVA_CANCELADA, motivo));
        r.cancelar(motivo.trim());
        avisar(r, "fue cancelada por la administración. Motivo: " + motivo.trim());
        return ReservaResponse.de(r);
    }

    @Override
    public ReservaResponse finalizar(Long id) {
        Reserva r = buscar(id);
        r.cambiarEstado(estados.transicionar(r.getEstado(), NombresEstado.RESERVA_FINALIZADA, null));
        return ReservaResponse.de(r);
    }

    /** CAMBIO_ESTADO_RESERVA para quien reservo. */
    private void avisar(Reserva r, String queOcurrio) {
        notificador.notificar(ETipoEvento.CAMBIO_ESTADO_RESERVA, List.of(r.getPersona()),
                "Su reserva de " + r.getZonaComun().getNombre() + " del " + r.getFecha() + " " + queOcurrio + ".");
    }

    private Reserva buscar(Long id) {
        return reservas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
    }
}
