package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ECategoriaInmueble;
import unisimon.apipropiedadhorizontal.domain.enums.EResultadoContacto;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoVigencia;
import unisimon.apipropiedadhorizontal.domain.model.Inmueble;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.PreRegistro;
import unisimon.apipropiedadhorizontal.domain.model.RegistroVisita;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.Visitante;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.InmuebleRepository;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.repository.PreRegistroRepository;
import unisimon.apipropiedadhorizontal.repository.RegistroVisitaRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.repository.VisitanteRepository;
import unisimon.apipropiedadhorizontal.service.IngresoVisitanteService;
import unisimon.apipropiedadhorizontal.service.Notificador;
import unisimon.apipropiedadhorizontal.service.Visitantes;
import unisimon.apipropiedadhorizontal.util.RelojLocal;
import unisimon.apipropiedadhorizontal.web.dto.IngresoSinPreRegistroRequest;
import unisimon.apipropiedadhorizontal.web.dto.InmuebleConResidentesResponse;
import unisimon.apipropiedadhorizontal.web.dto.PreRegistroResponse;
import unisimon.apipropiedadhorizontal.web.dto.RegistroVisitaResponse;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class IngresoVisitanteServiceImpl implements IngresoVisitanteService {

    private final PreRegistroRepository preRegistros;
    private final RegistroVisitaRepository registros;
    private final VisitanteRepository visitantesRepo;
    private final Visitantes visitantes;
    private final InmuebleRepository inmuebles;
    private final PersonaRepository personas;
    private final ValorCatalogoRepository catalogos;
    private final Notificador notificador;
    private final Clock reloj;

    public IngresoVisitanteServiceImpl(PreRegistroRepository preRegistros, RegistroVisitaRepository registros,
                                       VisitanteRepository visitantesRepo, Visitantes visitantes,
                                       InmuebleRepository inmuebles, PersonaRepository personas,
                                       ValorCatalogoRepository catalogos, Notificador notificador, Clock reloj) {
        this.preRegistros = preRegistros;
        this.registros = registros;
        this.visitantesRepo = visitantesRepo;
        this.visitantes = visitantes;
        this.inmuebles = inmuebles;
        this.personas = personas;
        this.catalogos = catalogos;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreRegistroResponse> preRegistrosVigentes(String numeroDocumento) {
        LocalDate hoy = RelojLocal.hoy(reloj);
        return visitantesRepo.findByNumeroDocumento(numeroDocumento.trim())
                .map(v -> preRegistros.findByVisitanteIdAndActivoTrue(v.getId()).stream()
                        .filter(p -> p.estaVigente(hoy))
                        .map(p -> PreRegistroResponse.de(p, hoy))
                        .toList())
                .orElse(List.of());
    }

    @Override
    public RegistroVisitaResponse autorizarConPreRegistro(Long preRegistroId) {
        PreRegistro pre = preRegistros.findById(preRegistroId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pre-registro", preRegistroId));
        if (!pre.estaVigente(RelojLocal.hoy(reloj))) {
            throw new ReglaNegocioException("El pre-registro no esta vigente (cancelado, ya usado o de otra fecha)");
        }
        if (!pre.getPersona().vinculoEstaVigente()) {
            throw new ReglaNegocioException("El residente que pre-registro ya no tiene vinculo vigente");
        }
        verificarNoEstaDentro(pre.getVisitante());
        RegistroVisita registro = RegistroVisita.builder()
                .preRegistro(pre).visitante(pre.getVisitante()).inmueble(pre.getInmueble())
                .tipoVehiculo(pre.getTipoVehiculo()).build();
        registro.autorizarIngreso();
        if (pre.getTipoVigencia() == ETipoVigencia.FECHA_ESPECIFICA) {
            pre.marcarUsado();
        }
        registros.save(registro);
        avisarIngreso(registro, pre.getPersona());
        return RegistroVisitaResponse.de(registro);
    }

    @Override
    public RegistroVisitaResponse autorizarSinPreRegistro(IngresoSinPreRegistroRequest s) {
        Inmueble inmueble = inmuebles.findById(s.inmuebleId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Inmueble", s.inmuebleId()));
        if (inmueble.getCategoria() != ECategoriaInmueble.UNIDAD_PRIVADA || !inmueble.isActivo()) {
            throw new ReglaNegocioException("Solo se puede visitar una unidad privada activa");
        }
        Persona residente = personas.findById(s.residenteContactadoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona", s.residenteContactadoId()));
        if (!residente.esResidenteDe(inmueble)) {
            throw new ReglaNegocioException("La persona contactada no es residente vigente de ese inmueble");
        }
        ValorCatalogo tipoVehiculo = s.tipoVehiculoId() == null ? null : catalogos.findById(s.tipoVehiculoId())
                .filter(v -> v.getTipo() == ETipoCatalogo.TIPO_VEHICULO && v.isActivo())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de vehiculo", s.tipoVehiculoId()));

        Visitante visitante = visitantes.obtenerOCrear(s.nombreCompleto(), s.numeroDocumento());
        verificarNoEstaDentro(visitante);
        RegistroVisita registro = RegistroVisita.builder()
                .visitante(visitante).inmueble(inmueble).tipoVehiculo(tipoVehiculo).build();
        registro.registrarContactoResidente(residente, s.respuestaResidente());
        if (s.respuestaResidente() == EResultadoContacto.APRUEBA) {
            registro.autorizarIngreso();
        } else {
            registro.negarIngreso(motivoDeNegacion(s));
        }
        registros.save(registro);
        if (registro.isIngresoAutorizado()) {
            avisarIngreso(registro, residente);
        }
        return RegistroVisitaResponse.de(registro);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InmuebleConResidentesResponse> directorio() {
        Map<Long, List<Persona>> residentes = personas.findAll().stream()
                .filter(p -> p.vinculoEstaVigente() && p.getTipoPersona().isEsResidente())
                .collect(Collectors.groupingBy(p -> p.getInmueble().getId()));
        return inmuebles.findByCategoria(ECategoriaInmueble.UNIDAD_PRIVADA).stream()
                .filter(i -> i.isActivo() && residentes.containsKey(i.getId()))
                .sorted(Comparator.comparing(Inmueble::getTorreBloque).thenComparing(Inmueble::getNumero))
                .map(i -> new InmuebleConResidentesResponse(i.getId(), i.getTorreBloque(), i.getNumero(),
                        residentes.get(i.getId()).stream()
                                .map(p -> new InmuebleConResidentesResponse.Residente(p.getId(),
                                        p.getNombres() + " " + p.getApellidos(), p.getTelefono()))
                                .toList()))
                .toList();
    }

    private void verificarNoEstaDentro(Visitante visitante) {
        if (registros.existsByVisitanteIdAndIngresoAutorizadoTrueAndFechaHoraEntradaIsNotNullAndFechaHoraSalidaIsNull(
                visitante.getId())) {
            throw new ConflictoException("El visitante ya se encuentra dentro del conjunto; registre su salida primero");
        }
    }

    private String motivoDeNegacion(IngresoSinPreRegistroRequest s) {
        if (s.motivoNegacion() != null && !s.motivoNegacion().isBlank()) {
            return s.motivoNegacion().trim();
        }
        return s.respuestaResidente() == EResultadoContacto.NO_CONTESTA
                ? "El residente no contesto" : "El residente no autorizo el ingreso";
    }

    private void avisarIngreso(RegistroVisita registro, Persona destinatario) {
        String hora = registro.getFechaHoraEntrada().format(DateTimeFormatter.ofPattern("HH:mm"));
        notificador.notificar(ETipoEvento.INGRESO_VISITANTE, List.of(destinatario),
                "Ingreso su visitante " + registro.getVisitante().getNombreCompleto() + " a las " + hora + ".");
    }
}
