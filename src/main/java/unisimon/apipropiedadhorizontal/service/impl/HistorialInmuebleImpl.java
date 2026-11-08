package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Component;
import unisimon.apipropiedadhorizontal.domain.model.Inmueble;
import unisimon.apipropiedadhorizontal.repository.CorrespondenciaRepository;
import unisimon.apipropiedadhorizontal.repository.InmuebleRepository;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.repository.PqrsRepository;
import unisimon.apipropiedadhorizontal.repository.PreRegistroRepository;
import unisimon.apipropiedadhorizontal.repository.RegistroVisitaRepository;
import unisimon.apipropiedadhorizontal.repository.ReservaRepository;
import unisimon.apipropiedadhorizontal.repository.VehiculoRepository;
import unisimon.apipropiedadhorizontal.service.HistorialInmueble;

@Component
public class HistorialInmuebleImpl implements HistorialInmueble {

    private final PersonaRepository personas;
    private final VehiculoRepository vehiculos;
    private final CorrespondenciaRepository correspondencias;
    private final PqrsRepository pqrs;
    private final ReservaRepository reservas;
    private final PreRegistroRepository preRegistros;
    private final RegistroVisitaRepository visitas;
    private final InmuebleRepository inmuebles;

    public HistorialInmuebleImpl(PersonaRepository personas, VehiculoRepository vehiculos,
                                 CorrespondenciaRepository correspondencias, PqrsRepository pqrs,
                                 ReservaRepository reservas, PreRegistroRepository preRegistros,
                                 RegistroVisitaRepository visitas, InmuebleRepository inmuebles) {
        this.personas = personas;
        this.vehiculos = vehiculos;
        this.correspondencias = correspondencias;
        this.pqrs = pqrs;
        this.reservas = reservas;
        this.preRegistros = preRegistros;
        this.visitas = visitas;
        this.inmuebles = inmuebles;
    }

    /** Haber tenido personas vinculadas (vigentes o pasadas) o parqueaderos asignados tambien cuenta como historia. */
    @Override
    public boolean tieneHistorial(Inmueble inmueble) {
        Long id = inmueble.getId();
        return personas.existsByInmuebleId(id)
                || inmuebles.existsByInmuebleAsociadoId(id)
                || vehiculos.existsByInmuebleId(id)
                || correspondencias.existsByInmuebleId(id)
                || pqrs.existsByInmuebleId(id)
                || reservas.existsByInmuebleId(id)
                || preRegistros.existsByInmuebleId(id)
                || visitas.existsByInmuebleId(id);
    }
}
