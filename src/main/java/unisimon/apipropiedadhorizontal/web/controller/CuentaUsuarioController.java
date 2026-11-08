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
import unisimon.apipropiedadhorizontal.service.CuentaUsuarioService;
import unisimon.apipropiedadhorizontal.web.dto.CrearCuentaRequest;
import unisimon.apipropiedadhorizontal.web.dto.CuentaResponse;

import java.util.List;

/** CU-005 Gestionar cuentas de usuario. */
@RestController
@RequestMapping("/api/cuentas")
public class CuentaUsuarioController {

    private final CuentaUsuarioService cuentas;

    public CuentaUsuarioController(CuentaUsuarioService cuentas) {
        this.cuentas = cuentas;
    }

    @SoloAdministrador
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaResponse crear(@Valid @RequestBody CrearCuentaRequest solicitud) {
        return cuentas.crear(solicitud);
    }

    @SoloAdministrador
    @GetMapping
    public List<CuentaResponse> listar() {
        return cuentas.listar();
    }

    @SoloAdministrador
    @GetMapping("/{id}")
    public CuentaResponse obtener(@PathVariable Long id) {
        return cuentas.obtener(id);
    }

    @SoloAdministrador
    @PatchMapping("/{id}/desactivar")
    public CuentaResponse desactivar(@PathVariable Long id) {
        return cuentas.desactivar(id);
    }

    @SoloAdministrador
    @PatchMapping("/{id}/reactivar")
    public CuentaResponse reactivar(@PathVariable Long id) {
        return cuentas.reactivar(id);
    }
}
