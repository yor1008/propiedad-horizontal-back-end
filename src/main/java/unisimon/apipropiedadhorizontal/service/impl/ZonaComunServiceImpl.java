package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.ZonaComun;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.MantenimientoRepository;
import unisimon.apipropiedadhorizontal.repository.ReservaRepository;
import unisimon.apipropiedadhorizontal.repository.ZonaComunRepository;
import unisimon.apipropiedadhorizontal.service.ZonaComunService;
import unisimon.apipropiedadhorizontal.web.dto.DisponibilidadResponse;
import unisimon.apipropiedadhorizontal.web.dto.HorarioDto;
import unisimon.apipropiedadhorizontal.web.dto.ZonaComunRequest;
import unisimon.apipropiedadhorizontal.web.dto.ZonaComunResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class ZonaComunServiceImpl implements ZonaComunService {

    private final ZonaComunRepository zonas;
    private final ReservaRepository reservas;
    private final MantenimientoRepository mantenimientos;

    public ZonaComunServiceImpl(ZonaComunRepository zonas, ReservaRepository reservas,
                               MantenimientoRepository mantenimientos) {
        this.zonas = zonas;
        this.reservas = reservas;
        this.mantenimientos = mantenimientos;
    }

    @Override
    public ZonaComunResponse crear(ZonaComunRequest s) {
        validar(s);
        String nombre = s.nombre().trim();
        if (zonas.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe una zona comun llamada " + nombre);
        }
        ZonaComun zona = ZonaComun.builder()
                .nombre(nombre).descripcion(s.descripcion()).capacidadMaxima(s.capacidadMaxima())
                .tipoUso(s.tipoUso()).requiereAprobacion(s.requiereAprobacion()).build();
        aplicarReglasYHorarios(zona, s);
        return ZonaComunResponse.de(zonas.save(zona));
    }

    @Override
    public ZonaComunResponse actualizar(Long id, ZonaComunRequest s) {
        validar(s);
        ZonaComun zona = buscar(id);
        String nombre = s.nombre().trim();
        if (zonas.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictoException("Ya existe una zona comun llamada " + nombre);
        }
        zona.modificarDatos(nombre, s.descripcion(), s.capacidadMaxima(), s.tipoUso(), s.requiereAprobacion());
        zona.limpiarHorarios();
        aplicarReglasYHorarios(zona, s);
        return ZonaComunResponse.de(zona);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZonaComunResponse> listar(boolean soloActivas) {
        List<ZonaComun> lista = soloActivas ? zonas.findByActivaTrueOrderByNombre() : zonas.findAllByOrderByNombre();
        return lista.stream().map(ZonaComunResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ZonaComunResponse obtener(Long id) {
        return ZonaComunResponse.de(buscar(id));
    }

    @Override
    public ZonaComunResponse desactivar(Long id, String motivo) {
        ZonaComun zona = buscar(id);
        zona.desactivar(motivo.trim());
        return ZonaComunResponse.de(zona);
    }

    @Override
    public ZonaComunResponse reactivar(Long id) {
        ZonaComun zona = buscar(id);
        zona.reactivar();
        return ZonaComunResponse.de(zona);
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadResponse disponibilidad(Long id, LocalDate fecha) {
        ZonaComun zona = buscar(id);
        List<HorarioDto> horarios = zona.consultarDisponibilidad(fecha).stream().map(HorarioDto::de).toList();
        List<DisponibilidadResponse.FranjaOcupada> ocupadas = reservas
                .findByZonaComunIdAndFechaAndEstadoEsFinalFalseOrderByHoraInicio(id, fecha).stream()
                .map(r -> new DisponibilidadResponse.FranjaOcupada(r.getHoraInicio(), r.getHoraFin(),
                        r.getEstado().getNombre()))
                .toList();
        List<DisponibilidadResponse.FranjaOcupada> todas = new ArrayList<>(ocupadas);
        todas.addAll(franjasDeMantenimiento(id, fecha));
        todas.sort(Comparator.comparing(DisponibilidadResponse.FranjaOcupada::horaInicio));
        return new DisponibilidadResponse(fecha, horarios, todas);
    }

    /** Mantenimientos vigentes de la zona recortados al dia consultado; estado "Mantenimiento". */
    private List<DisponibilidadResponse.FranjaOcupada> franjasDeMantenimiento(Long zonaId, LocalDate fecha) {
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();
        return mantenimientos.findByZonaComunIdAndEstadoEsFinalFalse(zonaId).stream()
                .filter(m -> m.afectaPeriodo(inicioDia, finDia))
                .map(m -> new DisponibilidadResponse.FranjaOcupada(
                        m.getFechaHoraInicio().isBefore(inicioDia) ? LocalTime.MIN : m.getFechaHoraInicio().toLocalTime(),
                        m.getFechaHoraFin() == null || !m.getFechaHoraFin().isBefore(finDia)
                                ? LocalTime.of(23, 59) : m.getFechaHoraFin().toLocalTime(),
                        "Mantenimiento"))
                .toList();
    }

    private void aplicarReglasYHorarios(ZonaComun zona, ZonaComunRequest s) {
        zona.configurarReglas(s.duracionMaximaMinutos(), s.anticipacionMinimaHoras(), s.anticipacionMaximaDias(),
                s.limiteCancelacionHoras(), s.maxReservasPorInmueble(), s.periodoLimite());
        s.horarios().forEach(h -> zona.agregarHorario(h.diaSemana(), h.horaInicio(), h.horaFin()));
    }

    private void validar(ZonaComunRequest s) {
        if (s.maxReservasPorInmueble() > 0 && s.periodoLimite() == null) {
            throw new ReglaNegocioException("periodoLimite es obligatorio cuando maxReservasPorInmueble es mayor que 0");
        }
        if (s.horarios().stream().anyMatch(h -> !h.horaFin().isAfter(h.horaInicio()))) {
            throw new ReglaNegocioException("En cada horario, horaFin debe ser posterior a horaInicio");
        }
    }

    private ZonaComun buscar(Long id) {
        return zonas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Zona comun", id));
    }

    @Override
    public void eliminar(Long id) {
        Borrado.fisico(zonas, buscar(id),
                "La zona tiene reservas o mantenimientos asociados y no se puede borrar; desactivela en su lugar");
    }
}
