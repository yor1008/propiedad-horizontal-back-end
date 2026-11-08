package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoVigencia;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.PreRegistro;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.Visitante;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.PreRegistroRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.service.PersonaAutenticada;
import unisimon.apipropiedadhorizontal.service.PreRegistroService;
import unisimon.apipropiedadhorizontal.service.Visitantes;
import unisimon.apipropiedadhorizontal.util.RelojLocal;
import unisimon.apipropiedadhorizontal.web.dto.CrearPreRegistroRequest;
import unisimon.apipropiedadhorizontal.web.dto.PreRegistroResponse;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional
public class PreRegistroServiceImpl implements PreRegistroService {

    private final PreRegistroRepository preRegistros;
    private final PersonaAutenticada personaAutenticada;
    private final Visitantes visitantes;
    private final ValorCatalogoRepository catalogos;
    private final Clock reloj;

    public PreRegistroServiceImpl(PreRegistroRepository preRegistros, PersonaAutenticada personaAutenticada,
                                  Visitantes visitantes, ValorCatalogoRepository catalogos, Clock reloj) {
        this.preRegistros = preRegistros;
        this.personaAutenticada = personaAutenticada;
        this.visitantes = visitantes;
        this.catalogos = catalogos;
        this.reloj = reloj;
    }

    @Override
    public PreRegistroResponse crear(CrearPreRegistroRequest s) {
        Persona persona = personaAutenticada.entidad();
        if (!persona.vinculoEstaVigente() || !persona.getTipoPersona().isEsResidente()) {
            throw new ReglaNegocioException("Solo un residente con vinculo vigente puede pre-registrar visitantes");
        }
        LocalDate hoy = RelojLocal.hoy(reloj);
        LocalDate fecha = null;
        if (s.tipoVigencia() == ETipoVigencia.FECHA_ESPECIFICA) {
            if (s.fechaPrevista() == null) {
                throw new ReglaNegocioException("fechaPrevista es obligatoria para un pre-registro de fecha especifica");
            }
            if (s.fechaPrevista().isBefore(hoy)) {
                throw new ReglaNegocioException("fechaPrevista no puede estar en el pasado");
            }
            fecha = s.fechaPrevista();
        }
        ValorCatalogo tipoVehiculo = s.tipoVehiculoId() == null ? null : catalogos.findById(s.tipoVehiculoId())
                .filter(v -> v.getTipo() == ETipoCatalogo.TIPO_VEHICULO && v.isActivo())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de vehiculo", s.tipoVehiculoId()));

        Visitante visitante = visitantes.obtenerOCrear(s.nombreCompleto(), s.numeroDocumento());
        LocalDate fechaFinal = fecha;
        boolean repetido = preRegistros.findByVisitanteIdAndInmuebleIdAndActivoTrue(visitante.getId(),
                        persona.getInmueble().getId()).stream()
                .anyMatch(e -> !e.isUsado() && e.getTipoVigencia() == s.tipoVigencia()
                        && Objects.equals(e.getFechaPrevista(), fechaFinal));
        if (repetido) {
            throw new ConflictoException("Ya existe un pre-registro vigente de ese visitante para su inmueble");
        }
        String placa = s.placaVehiculo() == null || s.placaVehiculo().isBlank() ? null
                : s.placaVehiculo().trim().toUpperCase(Locale.ROOT);
        PreRegistro nuevo = preRegistros.save(PreRegistro.builder()
                .persona(persona).visitante(visitante).inmueble(persona.getInmueble())
                .tipoVigencia(s.tipoVigencia()).fechaPrevista(fecha).motivo(s.motivo())
                .placaVehiculo(placa).tipoVehiculo(tipoVehiculo).build());
        return PreRegistroResponse.de(nuevo, hoy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreRegistroResponse> mios() {
        LocalDate hoy = RelojLocal.hoy(reloj);
        return preRegistros.findByPersonaIdOrderByFechaHoraRegistroDesc(personaAutenticada.id()).stream()
                .map(p -> PreRegistroResponse.de(p, hoy)).toList();
    }

    @Override
    public PreRegistroResponse cancelar(Long id) {
        Long personaId = personaAutenticada.id();
        PreRegistro p = preRegistros.findById(id)
                .filter(e -> e.getPersona().getId().equals(personaId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Pre-registro", id));
        if (!p.isActivo()) {
            throw new ReglaNegocioException("El pre-registro ya esta cancelado");
        }
        if (p.isUsado()) {
            throw new ReglaNegocioException("El pre-registro ya fue utilizado");
        }
        p.cancelar();
        return PreRegistroResponse.de(p, RelojLocal.hoy(reloj));
    }
}
