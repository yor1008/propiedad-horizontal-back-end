package unisimon.apipropiedadhorizontal.web.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloAdministrador;
import unisimon.apipropiedadhorizontal.service.MantenimientoService;
import unisimon.apipropiedadhorizontal.web.dto.ActualizarMantenimientoRequest;
import unisimon.apipropiedadhorizontal.web.dto.FinalizarMantenimientoRequest;
import unisimon.apipropiedadhorizontal.web.dto.MantenimientoResponse;
import unisimon.apipropiedadhorizontal.web.dto.MotivoRequest;
import unisimon.apipropiedadhorizontal.web.dto.ProgramarMantenimientoRequest;

import java.util.List;

/** CU-028 Gestionar mantenimientos. */
@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoController {

    private final MantenimientoService mantenimientos;

    public MantenimientoController(MantenimientoService mantenimientos) {
        this.mantenimientos = mantenimientos;
    }

    @SoloAdministrador
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MantenimientoResponse programar(@Valid @RequestBody ProgramarMantenimientoRequest solicitud) {
        return mantenimientos.programar(solicitud);
    }

    @SoloAdministrador
    @GetMapping
    public List<MantenimientoResponse> listar() {
        return mantenimientos.listar();
    }

    @SoloAdministrador
    @GetMapping("/{id}")
    public MantenimientoResponse obtener(@PathVariable Long id) {
        return mantenimientos.obtener(id);
    }

    @SoloAdministrador
    @PutMapping("/{id}")
    public MantenimientoResponse actualizar(@PathVariable Long id,
                                            @Valid @RequestBody ActualizarMantenimientoRequest solicitud) {
        return mantenimientos.actualizar(id, solicitud);
    }

    @SoloAdministrador
    @PatchMapping("/{id}/iniciar")
    public MantenimientoResponse iniciar(@PathVariable Long id) {
        return mantenimientos.iniciar(id);
    }

    @SoloAdministrador
    @PatchMapping("/{id}/finalizar")
    public MantenimientoResponse finalizar(@PathVariable Long id,
                                           @Valid @RequestBody(required = false) FinalizarMantenimientoRequest solicitud) {
        return mantenimientos.finalizar(id, solicitud == null ? null : solicitud.observaciones());
    }

    @SoloAdministrador
    @PatchMapping("/{id}/cancelar")
    public MantenimientoResponse cancelar(@PathVariable Long id, @Valid @RequestBody MotivoRequest solicitud) {
        return mantenimientos.cancelar(id, solicitud.motivo());
    }

    @SoloAdministrador
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        mantenimientos.eliminar(id);
    }
}
