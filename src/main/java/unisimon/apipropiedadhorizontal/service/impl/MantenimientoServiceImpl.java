package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.NombresEstado;
import unisimon.apipropiedadhorizontal.domain.enums.EProcesoEstado;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.Mantenimiento;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.ZonaComun;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.MantenimientoRepository;
import unisimon.apipropiedadhorizontal.repository.ReservaRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.repository.ZonaComunRepository;
import unisimon.apipropiedadhorizontal.service.GestorEstados;
import unisimon.apipropiedadhorizontal.service.MantenimientoService;
import unisimon.apipropiedadhorizontal.web.dto.ActualizarMantenimientoRequest;
import unisimon.apipropiedadhorizontal.web.dto.MantenimientoResponse;
import unisimon.apipropiedadhorizontal.web.dto.ProgramarMantenimientoRequest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class MantenimientoServiceImpl implements MantenimientoService {

    /** Tolerancia para que programar "ahora mismo" no falle por segundos de diferencia con el front. */
    private static final long TOLERANCIA_MINUTOS = 5;

    private final MantenimientoRepository mantenimientos;
    private final ZonaComunRepository zonas;
    private final ValorCatalogoRepository catalogos;
    private final ReservaRepository reservas;
    private final GestorEstados estados;
    private final Clock reloj;

    public MantenimientoServiceImpl(MantenimientoRepository mantenimientos, ZonaComunRepository zonas,
                                    ValorCatalogoRepository catalogos, ReservaRepository reservas,
                                    GestorEstados estados, Clock reloj) {
        this.mantenimientos = mantenimientos;
        this.zonas = zonas;
        this.catalogos = catalogos;
        this.reservas = reservas;
        this.estados = estados;
        this.reloj = reloj;
    }

    @Override
    public MantenimientoResponse programar(ProgramarMantenimientoRequest s) {
        validarFechas(s.fechaHoraInicio(), s.fechaHoraFin());
        ZonaComun zona = s.zonaComunId() == null ? null : zonas.findById(s.zonaComunId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Zona comun", s.zonaComunId()));
        ValorCatalogo tipo = catalogos.findById(s.tipoId())
                .filter(v -> v.getTipo() == ETipoCatalogo.TIPO_MANTENIMIENTO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de mantenimiento", s.tipoId()));
        if (!tipo.isActivo()) {
            throw new ReglaNegocioException("El tipo de mantenimiento esta inactivo");
        }
        if (zona != null) {
            verificarSinReservas(zona.getId(), s.fechaHoraInicio(), s.fechaHoraFin());
        }
        Mantenimiento m = mantenimientos.save(Mantenimiento.builder()
                .zonaComun(zona).tipo(tipo).elementoArea(s.elementoArea().trim()).descripcion(s.descripcion())
                .fechaHoraInicio(s.fechaHoraInicio()).fechaHoraFin(s.fechaHoraFin()).responsable(s.responsable())
                .estado(estados.inicial(EProcesoEstado.MANTENIMIENTO)).build());
        return MantenimientoResponse.de(m);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MantenimientoResponse> listar() {
        return mantenimientos.findAllByOrderByFechaHoraInicioDesc().stream().map(MantenimientoResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MantenimientoResponse obtener(Long id) {
        return MantenimientoResponse.de(buscar(id));
    }

    @Override
    public MantenimientoResponse actualizar(Long id, ActualizarMantenimientoRequest s) {
        Mantenimiento m = buscar(id);
        if (!m.getEstado().isEsInicial()) {
            throw new ReglaNegocioException("Solo se puede modificar un mantenimiento programado");
        }
        validarFechas(s.fechaHoraInicio(), s.fechaHoraFin());
        if (m.getZonaComun() != null) {
            verificarSinReservas(m.getZonaComun().getId(), s.fechaHoraInicio(), s.fechaHoraFin());
        }
        m.modificarDatos(s.descripcion(), s.fechaHoraInicio(), s.fechaHoraFin(), s.responsable());
        return MantenimientoResponse.de(m);
    }

    @Override
    public MantenimientoResponse iniciar(Long id) {
        Mantenimiento m = buscar(id);
        m.cambiarEstado(estados.transicionar(m.getEstado(), NombresEstado.MANTENIMIENTO_EN_CURSO, null));
        return MantenimientoResponse.de(m);
    }

    /** Si el mantenimiento tenia periodo abierto, su fin queda en el momento de finalizarlo. */
    @Override
    public MantenimientoResponse finalizar(Long id, String observaciones) {
        Mantenimiento m = buscar(id);
        m.cambiarEstado(estados.transicionar(m.getEstado(), NombresEstado.MANTENIMIENTO_COMPLETADO, null));
        m.finalizar(observaciones == null || observaciones.isBlank() ? null : observaciones.trim());
        if (m.getFechaHoraFin() == null) {
            m.modificarDatos(m.getDescripcion(), m.getFechaHoraInicio(), ahora(), m.getResponsable());
        }
        return MantenimientoResponse.de(m);
    }

    @Override
    public MantenimientoResponse cancelar(Long id, String motivo) {
        Mantenimiento m = buscar(id);
        m.cambiarEstado(estados.transicionar(m.getEstado(), NombresEstado.MANTENIMIENTO_CANCELADO, motivo));
        m.cancelar(motivo.trim());
        return MantenimientoResponse.de(m);
    }

    private void validarFechas(LocalDateTime inicio, LocalDateTime fin) {
        if (fin != null && !fin.isAfter(inicio)) {
            throw new ReglaNegocioException("fechaHoraFin debe ser posterior a fechaHoraInicio");
        }
        if (inicio.isBefore(ahora().minusMinutes(TOLERANCIA_MINUTOS))) {
            throw new ReglaNegocioException("fechaHoraInicio no puede estar en el pasado");
        }
    }

    /** No se bloquea una zona que ya tiene reservas vigentes en el periodo: el administrador debe anularlas antes. */
    private void verificarSinReservas(Long zonaId, LocalDateTime inicio, LocalDateTime fin) {
        var hasta = fin == null ? inicio.toLocalDate().plusYears(10) : fin.toLocalDate();
        long afectadas = reservas.findByZonaComunIdAndFechaBetweenAndEstadoEsFinalFalse(zonaId, inicio.toLocalDate(), hasta)
                .stream()
                .filter(r -> inicio.isBefore(LocalDateTime.of(r.getFecha(), r.getHoraFin()))
                        && (fin == null || fin.isAfter(LocalDateTime.of(r.getFecha(), r.getHoraInicio()))))
                .count();
        if (afectadas > 0) {
            throw new ConflictoException("Hay " + afectadas + " reserva(s) vigente(s) en ese periodo; "
                    + "anulelas antes de programar el mantenimiento");
        }
    }

    /** Hora local del sistema, la misma en que las reservas y el front expresan fechas y horas. */
    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj.withZone(ZoneId.systemDefault()));
    }

    private Mantenimiento buscar(Long id) {
        return mantenimientos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Mantenimiento", id));
    }
}
