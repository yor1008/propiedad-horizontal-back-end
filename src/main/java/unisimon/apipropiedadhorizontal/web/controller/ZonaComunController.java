package unisimon.apipropiedadhorizontal.web.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import unisimon.apipropiedadhorizontal.security.anotaciones.Autenticado;
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloAdministrador;
import unisimon.apipropiedadhorizontal.service.ZonaComunService;
import unisimon.apipropiedadhorizontal.web.dto.DisponibilidadResponse;
import unisimon.apipropiedadhorizontal.web.dto.MotivoRequest;
import unisimon.apipropiedadhorizontal.web.dto.ZonaComunRequest;
import unisimon.apipropiedadhorizontal.web.dto.ZonaComunResponse;

import java.time.LocalDate;
import java.util.List;

/** CU-024 Gestionar zonas comunes (escritura: administrador; lectura: cualquier autenticado). */
@RestController
@RequestMapping("/api/zonas-comunes")
public class ZonaComunController {

    private final ZonaComunService zonas;

    public ZonaComunController(ZonaComunService zonas) {
        this.zonas = zonas;
    }

    @SoloAdministrador
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZonaComunResponse crear(@Valid @RequestBody ZonaComunRequest solicitud) {
        return zonas.crear(solicitud);
    }

    @Autenticado
    @GetMapping
    public List<ZonaComunResponse> listarActivas() {
        return zonas.listar(true);
    }

    @SoloAdministrador
    @GetMapping("/todas")
    public List<ZonaComunResponse> listarTodas() {
        return zonas.listar(false);
    }

    @Autenticado
    @GetMapping("/{id}")
    public ZonaComunResponse obtener(@PathVariable Long id) {
        return zonas.obtener(id);
    }

    @SoloAdministrador
    @PutMapping("/{id}")
    public ZonaComunResponse actualizar(@PathVariable Long id, @Valid @RequestBody ZonaComunRequest solicitud) {
        return zonas.actualizar(id, solicitud);
    }

    @SoloAdministrador
    @PatchMapping("/{id}/desactivar")
    public ZonaComunResponse desactivar(@PathVariable Long id, @Valid @RequestBody MotivoRequest solicitud) {
        return zonas.desactivar(id, solicitud.motivo());
    }

    @SoloAdministrador
    @PatchMapping("/{id}/reactivar")
    public ZonaComunResponse reactivar(@PathVariable Long id) {
        return zonas.reactivar(id);
    }

    @Autenticado
    @GetMapping("/{id}/disponibilidad")
    public DisponibilidadResponse disponibilidad(@PathVariable Long id,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return zonas.disponibilidad(id, fecha);
    }
}
