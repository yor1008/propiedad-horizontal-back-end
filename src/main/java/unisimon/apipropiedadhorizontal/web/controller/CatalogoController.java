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
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.security.anotaciones.Autenticado;
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloAdministrador;
import unisimon.apipropiedadhorizontal.service.CatalogoService;
import unisimon.apipropiedadhorizontal.web.dto.CatalogoRequest;
import unisimon.apipropiedadhorizontal.web.dto.CatalogoResponse;
import unisimon.apipropiedadhorizontal.web.dto.NombreRequest;

import java.util.List;

/** CU-006 Administrar catalogos. Lectura de valores activos para cualquier usuario; escritura solo Administrador. */
@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    private final CatalogoService catalogos;

    public CatalogoController(CatalogoService catalogos) {
        this.catalogos = catalogos;
    }

    @Autenticado
    @GetMapping("/{tipo}")
    public List<CatalogoResponse> listarActivos(@PathVariable ETipoCatalogo tipo) {
        return catalogos.listar(tipo, true);
    }

    @SoloAdministrador
    @GetMapping("/{tipo}/todos")
    public List<CatalogoResponse> listarTodos(@PathVariable ETipoCatalogo tipo) {
        return catalogos.listar(tipo, false);
    }

    @SoloAdministrador
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogoResponse crear(@Valid @RequestBody CatalogoRequest solicitud) {
        return catalogos.crear(solicitud);
    }

    @SoloAdministrador
    @PutMapping("/{id}")
    public CatalogoResponse renombrar(@PathVariable Long id, @Valid @RequestBody NombreRequest solicitud) {
        return catalogos.renombrar(id, solicitud.nombre());
    }

    @SoloAdministrador
    @PatchMapping("/{id}/desactivar")
    public CatalogoResponse desactivar(@PathVariable Long id) {
        return catalogos.desactivar(id);
    }

    @SoloAdministrador
    @PatchMapping("/{id}/reactivar")
    public CatalogoResponse reactivar(@PathVariable Long id) {
        return catalogos.reactivar(id);
    }
}
