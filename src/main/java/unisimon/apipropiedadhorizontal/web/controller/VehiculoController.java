package unisimon.apipropiedadhorizontal.web.controller;

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
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloVigilante;
import unisimon.apipropiedadhorizontal.service.VehiculoService;
import unisimon.apipropiedadhorizontal.web.dto.ActualizarVehiculoRequest;
import unisimon.apipropiedadhorizontal.web.dto.CrearVehiculoRequest;
import unisimon.apipropiedadhorizontal.web.dto.MotivoRequest;
import unisimon.apipropiedadhorizontal.web.dto.VehiculoResponse;

import java.util.List;

/** CU-012 Gestionar vehiculos y CU-013 Consultar vehiculo por placa. */
@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculos;

    public VehiculoController(VehiculoService vehiculos) {
        this.vehiculos = vehiculos;
    }

    @SoloAdministrador
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculoResponse crear(@Valid @RequestBody CrearVehiculoRequest solicitud) {
        return vehiculos.crear(solicitud);
    }

    @SoloAdministrador
    @GetMapping
    public List<VehiculoResponse> listar() {
        return vehiculos.listar();
    }

    @SoloAdministrador
    @GetMapping("/{id}")
    public VehiculoResponse obtener(@PathVariable Long id) {
        return vehiculos.obtener(id);
    }

    @SoloAdministrador
    @PutMapping("/{id}")
    public VehiculoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarVehiculoRequest solicitud) {
        return vehiculos.actualizar(id, solicitud);
    }

    /** Reutiliza el DTO de motivo (un solo campo obligatorio). */
    @SoloAdministrador
    @PatchMapping("/{id}/desactivar")
    public VehiculoResponse desactivar(@PathVariable Long id, @Valid @RequestBody MotivoRequest solicitud) {
        return vehiculos.desactivar(id, solicitud.motivo());
    }

    @SoloVigilante
    @GetMapping("/placa/{placa}")
    public VehiculoResponse porPlaca(@PathVariable String placa) {
        return vehiculos.consultarPorPlaca(placa);
    }
}
