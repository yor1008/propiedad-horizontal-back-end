package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionGeneral;
import unisimon.apipropiedadhorizontal.domain.model.Inmueble;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.Vehiculo;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.ConfiguracionGeneralRepository;
import unisimon.apipropiedadhorizontal.repository.InmuebleRepository;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.repository.VehiculoRepository;
import unisimon.apipropiedadhorizontal.service.VehiculoService;
import unisimon.apipropiedadhorizontal.web.dto.ActualizarVehiculoRequest;
import unisimon.apipropiedadhorizontal.web.dto.CrearVehiculoRequest;
import unisimon.apipropiedadhorizontal.web.dto.VehiculoResponse;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class VehiculoServiceImpl implements VehiculoService {

    private final VehiculoRepository vehiculos;
    private final PersonaRepository personas;
    private final InmuebleRepository inmuebles;
    private final ValorCatalogoRepository catalogos;
    private final ConfiguracionGeneralRepository configuracion;

    public VehiculoServiceImpl(VehiculoRepository vehiculos, PersonaRepository personas, InmuebleRepository inmuebles,
                               ValorCatalogoRepository catalogos, ConfiguracionGeneralRepository configuracion) {
        this.vehiculos = vehiculos;
        this.personas = personas;
        this.inmuebles = inmuebles;
        this.catalogos = catalogos;
        this.configuracion = configuracion;
    }

    @Override
    public VehiculoResponse crear(CrearVehiculoRequest s) {
        String placa = normalizarPlaca(s.placa());
        if (vehiculos.findByPlacaIgnoreCase(placa).isPresent()) {
            throw new ConflictoException("Ya existe un vehiculo con la placa " + placa);
        }
        Persona persona = personas.findById(s.personaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona", s.personaId()));
        Inmueble inmueble = inmuebles.findById(s.inmuebleId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Inmueble", s.inmuebleId()));
        if (!persona.vinculoEstaVigente() || !inmueble.equals(persona.getInmueble())) {
            throw new ReglaNegocioException("La persona debe tener un vinculo vigente con el inmueble");
        }
        int maximo = configuracion.findAll().stream().findFirst()
                .map(ConfiguracionGeneral::getMaxVehiculosPorInmueble).orElse(0);
        if (maximo > 0 && vehiculos.countByInmuebleIdAndActivoTrue(inmueble.getId()) >= maximo) {
            throw new ReglaNegocioException("El inmueble ya alcanzo el maximo de vehiculos (" + maximo + ")");
        }
        Vehiculo vehiculo = vehiculos.save(Vehiculo.builder()
                .persona(persona).inmueble(inmueble).placa(placa)
                .tipo(tipoVehiculo(s.tipoId())).marca(s.marca()).color(s.color()).build());
        return VehiculoResponse.de(vehiculo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> listar() {
        return vehiculos.findAll().stream().map(VehiculoResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehiculoResponse obtener(Long id) {
        return VehiculoResponse.de(buscar(id));
    }

    @Override
    public VehiculoResponse actualizar(Long id, ActualizarVehiculoRequest s) {
        Vehiculo vehiculo = buscar(id);
        vehiculo.modificarDatos(tipoVehiculo(s.tipoId()), s.marca(), s.color());
        return VehiculoResponse.de(vehiculo);
    }

    @Override
    public VehiculoResponse desactivar(Long id, String motivo) {
        Vehiculo vehiculo = buscar(id);
        vehiculo.desactivar(motivo.trim());
        return VehiculoResponse.de(vehiculo);
    }

    @Override
    @Transactional(readOnly = true)
    public VehiculoResponse consultarPorPlaca(String placa) {
        Vehiculo vehiculo = vehiculos.findByPlacaIgnoreCase(normalizarPlaca(placa))
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehiculo con placa", placa));
        return VehiculoResponse.de(vehiculo);
    }

    private Vehiculo buscar(Long id) {
        return vehiculos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Vehiculo", id));
    }

    private ValorCatalogo tipoVehiculo(Long id) {
        ValorCatalogo tipo = catalogos.findById(id)
                .filter(v -> v.getTipo() == ETipoCatalogo.TIPO_VEHICULO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de vehiculo", id));
        if (!tipo.isActivo()) {
            throw new ReglaNegocioException("El tipo de vehiculo esta inactivo");
        }
        return tipo;
    }

    private static String normalizarPlaca(String placa) {
        return placa.trim().toUpperCase(Locale.ROOT);
    }
}
