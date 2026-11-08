package unisimon.apipropiedadhorizontal.web.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import unisimon.apipropiedadhorizontal.security.anotaciones.AdministradorOVigilante;
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloVigilante;
import unisimon.apipropiedadhorizontal.service.HistorialAccesoService;
import unisimon.apipropiedadhorizontal.service.IngresoVisitanteService;
import unisimon.apipropiedadhorizontal.service.ParqueaderoVisitanteService;
import unisimon.apipropiedadhorizontal.service.SalidaVisitanteService;
import unisimon.apipropiedadhorizontal.web.dto.AsignarParqueaderoVisitanteRequest;
import unisimon.apipropiedadhorizontal.web.dto.IngresoConPreRegistroRequest;
import unisimon.apipropiedadhorizontal.web.dto.IngresoSinPreRegistroRequest;
import unisimon.apipropiedadhorizontal.web.dto.InmuebleConResidentesResponse;
import unisimon.apipropiedadhorizontal.web.dto.ParqueaderoResponse;
import unisimon.apipropiedadhorizontal.web.dto.PreRegistroResponse;
import unisimon.apipropiedadhorizontal.web.dto.RegistroVisitaResponse;

import java.time.LocalDate;
import java.util.List;

/** CU-016 a CU-021: control de acceso de visitantes (vigilante; el historial tambien lo ve el administrador). */
@RestController
@RequestMapping("/api/accesos")
public class AccesoController {

    private final IngresoVisitanteService ingresos;
    private final ParqueaderoVisitanteService parqueaderos;
    private final SalidaVisitanteService salidas;
    private final HistorialAccesoService historial;

    public AccesoController(IngresoVisitanteService ingresos, ParqueaderoVisitanteService parqueaderos,
                            SalidaVisitanteService salidas, HistorialAccesoService historial) {
        this.ingresos = ingresos;
        this.parqueaderos = parqueaderos;
        this.salidas = salidas;
        this.historial = historial;
    }

    @SoloVigilante
    @GetMapping("/pre-registros")
    public List<PreRegistroResponse> preRegistrosVigentes(@RequestParam String documento) {
        return ingresos.preRegistrosVigentes(documento);
    }

    @SoloVigilante
    @PostMapping("/con-preregistro")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroVisitaResponse conPreRegistro(@Valid @RequestBody IngresoConPreRegistroRequest solicitud) {
        return ingresos.autorizarConPreRegistro(solicitud.preRegistroId());
    }

    @SoloVigilante
    @PostMapping("/sin-preregistro")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroVisitaResponse sinPreRegistro(@Valid @RequestBody IngresoSinPreRegistroRequest solicitud) {
        return ingresos.autorizarSinPreRegistro(solicitud);
    }

    @SoloVigilante
    @GetMapping("/inmuebles")
    public List<InmuebleConResidentesResponse> directorio() {
        return ingresos.directorio();
    }

    @SoloVigilante
    @GetMapping("/parqueaderos-disponibles")
    public List<ParqueaderoResponse> parqueaderosDisponibles() {
        return parqueaderos.disponibles();
    }

    @SoloVigilante
    @PatchMapping("/{id}/parqueadero")
    public RegistroVisitaResponse asignarParqueadero(@PathVariable Long id,
                                                     @Valid @RequestBody AsignarParqueaderoVisitanteRequest solicitud) {
        return parqueaderos.asignar(id, solicitud.parqueaderoId());
    }

    @SoloVigilante
    @PatchMapping("/{id}/salida")
    public RegistroVisitaResponse registrarSalida(@PathVariable Long id) {
        return salidas.registrarSalida(id);
    }

    @SoloVigilante
    @GetMapping("/dentro")
    public List<RegistroVisitaResponse> dentro() {
        return salidas.dentro();
    }

    @AdministradorOVigilante
    @GetMapping("/historial")
    public List<RegistroVisitaResponse> historial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String documento) {
        return historial.consultar(desde, hasta, documento);
    }
}
