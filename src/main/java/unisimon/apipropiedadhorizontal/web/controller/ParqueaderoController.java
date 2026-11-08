package unisimon.apipropiedadhorizontal.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloAdministrador;
import unisimon.apipropiedadhorizontal.service.ParqueaderoService;
import unisimon.apipropiedadhorizontal.web.dto.AsignarParqueaderoRequest;
import unisimon.apipropiedadhorizontal.web.dto.CrearParqueaderoRequest;
import unisimon.apipropiedadhorizontal.web.dto.MotivoRequest;
import unisimon.apipropiedadhorizontal.web.dto.ParqueaderoResponse;

import java.util.List;

/** CU-014 Gestionar parqueaderos. */
@RestController
@RequestMapping("/api/parqueaderos")
public class ParqueaderoController {

    private final ParqueaderoService parqueaderos;

    public ParqueaderoController(ParqueaderoService parqueaderos) {
        this.parqueaderos = parqueaderos;
    }

    @SoloAdministrador
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParqueaderoResponse crear(@Valid @RequestBody CrearParqueaderoRequest solicitud) {
        return parqueaderos.crear(solicitud);
    }

    @SoloAdministrador
    @GetMapping
    public List<ParqueaderoResponse> listar() {
        return parqueaderos.listar();
    }

    @SoloAdministrador
    @PatchMapping("/{id}/asignar")
    public ParqueaderoResponse asignar(@PathVariable Long id, @Valid @RequestBody AsignarParqueaderoRequest solicitud) {
        return parqueaderos.asignar(id, solicitud.inmuebleId());
    }

    @SoloAdministrador
    @PatchMapping("/{id}/liberar")
    public ParqueaderoResponse liberar(@PathVariable Long id) {
        return parqueaderos.liberar(id);
    }

    @SoloAdministrador
    @PatchMapping("/{id}/desactivar")
    public ParqueaderoResponse desactivar(@PathVariable Long id, @Valid @RequestBody MotivoRequest solicitud) {
        return parqueaderos.desactivar(id, solicitud.motivo());
    }
}
