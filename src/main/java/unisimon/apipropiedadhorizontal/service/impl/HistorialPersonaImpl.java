package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Component;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.repository.CorrespondenciaRepository;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;
import unisimon.apipropiedadhorizontal.repository.NotificacionRepository;
import unisimon.apipropiedadhorizontal.repository.PqrsRepository;
import unisimon.apipropiedadhorizontal.repository.PreRegistroRepository;
import unisimon.apipropiedadhorizontal.repository.RegistroVisitaRepository;
import unisimon.apipropiedadhorizontal.repository.ReservaRepository;
import unisimon.apipropiedadhorizontal.repository.VehiculoRepository;
import unisimon.apipropiedadhorizontal.service.HistorialPersona;

@Component
public class HistorialPersonaImpl implements HistorialPersona {

    private final CuentaUsuarioRepository cuentas;
    private final VehiculoRepository vehiculos;
    private final CorrespondenciaRepository correspondencias;
    private final PqrsRepository pqrs;
    private final ReservaRepository reservas;
    private final PreRegistroRepository preRegistros;
    private final NotificacionRepository notificaciones;
    private final RegistroVisitaRepository visitas;

    public HistorialPersonaImpl(CuentaUsuarioRepository cuentas, VehiculoRepository vehiculos,
                                CorrespondenciaRepository correspondencias, PqrsRepository pqrs,
                                ReservaRepository reservas, PreRegistroRepository preRegistros,
                                NotificacionRepository notificaciones, RegistroVisitaRepository visitas) {
        this.cuentas = cuentas;
        this.vehiculos = vehiculos;
        this.correspondencias = correspondencias;
        this.pqrs = pqrs;
        this.reservas = reservas;
        this.preRegistros = preRegistros;
        this.notificaciones = notificaciones;
        this.visitas = visitas;
    }

    /** Tener o haber tenido un vinculo con un inmueble tambien cuenta como historia. */
    @Override
    public boolean tieneHistorial(Persona persona) {
        Long id = persona.getId();
        return persona.getInmueble() != null
                || cuentas.existsByPersonaId(id)
                || vehiculos.existsByPersonaId(id)
                || correspondencias.existsByPersonaId(id)
                || pqrs.existsByPersonaId(id)
                || reservas.existsByPersonaId(id)
                || preRegistros.existsByPersonaId(id)
                || notificaciones.existsByPersonaId(id)
                || visitas.existsByResidenteContactadoId(id);
    }
}
