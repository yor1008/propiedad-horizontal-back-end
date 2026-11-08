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
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloPersonaVinculada;
import unisimon.apipropiedadhorizontal.service.PqrsService;
import unisimon.apipropiedadhorizontal.web.dto.AtenderPqrsRequest;
import unisimon.apipropiedadhorizontal.web.dto.PqrsResponse;
import unisimon.apipropiedadhorizontal.web.dto.RadicarPqrsRequest;

import java.util.List;

/** CU-029 Radicar, CU-030 Consultar propias, CU-031 Atender PQRS. */
@RestController
@RequestMapping("/api/pqrs")
public class PqrsController {

    private final PqrsService pqrs;

    public PqrsController(PqrsService pqrs) {
        this.pqrs = pqrs;
    }

    @SoloPersonaVinculada
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PqrsResponse radicar(@Valid @RequestBody RadicarPqrsRequest solicitud) {
        return pqrs.radicar(solicitud);
    }

    @SoloPersonaVinculada
    @GetMapping("/mias")
    public List<PqrsResponse> mias() {
        return pqrs.mias();
    }

    @SoloAdministrador
    @GetMapping
    public List<PqrsResponse> listar() {
        return pqrs.listar();
    }

    @SoloAdministrador
    @PatchMapping("/{id}/atender")
    public PqrsResponse atender(@PathVariable Long id, @Valid @RequestBody AtenderPqrsRequest solicitud) {
        return pqrs.atender(id, solicitud);
    }
}
