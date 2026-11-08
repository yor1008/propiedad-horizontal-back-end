package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.NombresEstado;
import unisimon.apipropiedadhorizontal.domain.enums.EProcesoEstado;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.Reserva;
import unisimon.apipropiedadhorizontal.domain.model.ZonaComun;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.MantenimientoRepository;
import unisimon.apipropiedadhorizontal.repository.ReservaRepository;
import unisimon.apipropiedadhorizontal.repository.ZonaComunRepository;
import unisimon.apipropiedadhorizontal.service.GestorEstados;
import unisimon.apipropiedadhorizontal.service.PersonaAutenticada;
import unisimon.apipropiedadhorizontal.service.ReservaService;
import unisimon.apipropiedadhorizontal.web.dto.ReservaResponse;
import unisimon.apipropiedadhorizontal.web.dto.ReservarRequest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Transactional
public class ReservaServiceImpl implements ReservaService {

    private static final List<String> NO_CUENTAN = List.of(NombresEstado.RESERVA_RECHAZADA, NombresEstado.RESERVA_CANCELADA);

    private final ReservaRepository reservas;
    private final ZonaComunRepository zonas;
    private final GestorEstados estados;
    private final PersonaAutenticada personaAutenticada;
    private final MantenimientoRepository mantenimientos;

    public ReservaServiceImpl(ReservaRepository reservas, ZonaComunRepository zonas, GestorEstados estados,
                              PersonaAutenticada personaAutenticada,
                              MantenimientoRepository mantenimientos) {
        this.reservas = reservas;
        this.zonas = zonas;
        this.estados = estados;
        this.personaAutenticada = personaAutenticada;
        this.mantenimientos = mantenimientos;
    }

    @Override
    public ReservaResponse reservar(ReservarRequest s) {
        Persona persona = personaAutenticada.entidad();
        if (!persona.vinculoEstaVigente()) {
            throw new ReglaNegocioException("Necesita un vinculo vigente con un inmueble para reservar");
        }
        ZonaComun zona = zonas.findById(s.zonaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Zona comun", s.zonaId()));
        if (!zona.isActiva()) {
            throw new ReglaNegocioException("La zona comun no esta disponible");
        }
        if (!s.horaFin().isAfter(s.horaInicio())) {
            throw new ReglaNegocioException("horaFin debe ser posterior a horaInicio");
        }
        Reserva reserva = Reserva.builder()
                .persona(persona).inmueble(persona.getInmueble()).zonaComun(zona)
                .fecha(s.fecha()).horaInicio(s.horaInicio()).horaFin(s.horaFin())
                .numeroAsistentes(s.numeroAsistentes()).motivo(s.motivo())
                .estado(estados.inicial(EProcesoEstado.RESERVA)).build();
        if (!zona.validarReserva(reserva)) {
            throw new ReglaNegocioException("La reserva incumple las reglas de la zona "
                    + "(horario disponible, capacidad de " + zona.getCapacidadMaxima() + ", duracion o anticipacion)");
        }
        if (!reservas.buscarTraslapadas(zona.getId(), s.fecha(), s.horaInicio(), s.horaFin()).isEmpty()) {
            throw new ConflictoException("Ya hay una reserva en ese horario");
        }
        LocalDateTime desde = LocalDateTime.of(s.fecha(), s.horaInicio());
        LocalDateTime hasta = LocalDateTime.of(s.fecha(), s.horaFin());
        if (mantenimientos.findByZonaComunIdAndEstadoEsFinalFalse(zona.getId()).stream()
                .anyMatch(m -> m.afectaPeriodo(desde, hasta))) {
            throw new ConflictoException("La zona tiene un mantenimiento programado en ese horario");
        }
        verificarLimitePorInmueble(zona, persona, s.fecha());
        if (!zona.isRequiereAprobacion()) {
            reserva.cambiarEstado(estados.transicionar(reserva.getEstado(), NombresEstado.RESERVA_APROBADA, null));
            reserva.aprobar(null);
        }
        return ReservaResponse.de(reservas.save(reserva));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> mias() {
        return reservas.findByPersonaIdOrderByFechaDesc(personaAutenticada.id()).stream()
                .map(ReservaResponse::de).toList();
    }

    @Override
    public ReservaResponse cancelar(Long id, String motivo) {
        Long personaId = personaAutenticada.id();
        Reserva reserva = reservas.findById(id)
                .filter(r -> r.getPersona().getId().equals(personaId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
        if (!reserva.puedeCancelarResidente()) {
            throw new ReglaNegocioException("Ya paso el plazo para cancelar esta reserva ("
                    + reserva.getZonaComun().getLimiteCancelacionHoras() + " horas antes del inicio)");
        }
        reserva.cambiarEstado(estados.transicionar(reserva.getEstado(), NombresEstado.RESERVA_CANCELADA, motivo));
        reserva.cancelar(motivo.trim());
        return ReservaResponse.de(reserva);
    }

    /** Limite de reservas del inmueble en la zona por dia, semana (lunes a domingo) o mes. */
    private void verificarLimitePorInmueble(ZonaComun zona, Persona persona, LocalDate fecha) {
        if (zona.getMaxReservasPorInmueble() <= 0 || zona.getPeriodoLimite() == null) {
            return;
        }
        LocalDate desde;
        LocalDate hasta;
        switch (zona.getPeriodoLimite()) {
            case DIA -> {
                desde = fecha;
                hasta = fecha;
            }
            case SEMANA -> {
                desde = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                hasta = desde.plusDays(6);
            }
            default -> {
                desde = fecha.withDayOfMonth(1);
                hasta = fecha.with(TemporalAdjusters.lastDayOfMonth());
            }
        }
        long existentes = reservas.contarEnPeriodo(persona.getInmueble().getId(), zona.getId(), desde, hasta, NO_CUENTAN);
        if (existentes >= zona.getMaxReservasPorInmueble()) {
            throw new ReglaNegocioException("Su inmueble ya alcanzo el maximo de " + zona.getMaxReservasPorInmueble()
                    + " reserva(s) por " + zona.getPeriodoLimite().name().toLowerCase() + " en esta zona");
        }
    }
}
