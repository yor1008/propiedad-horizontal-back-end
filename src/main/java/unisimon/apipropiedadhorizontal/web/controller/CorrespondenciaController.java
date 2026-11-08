package unisimon.apipropiedadhorizontal.web.controller;

import jakarta.validation.Valid;
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
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloPersonaVinculada;
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloVigilante;
import unisimon.apipropiedadhorizontal.service.CorrespondenciaService;
import unisimon.apipropiedadhorizontal.web.dto.CorrespondenciaResponse;
import unisimon.apipropiedadhorizontal.web.dto.EntregarCorrespondenciaRequest;
import unisimon.apipropiedadhorizontal.web.dto.RegistrarCorrespondenciaRequest;

import java.util.List;

/** CU-022 Gestionar correspondencia y CU-023 Consultar correspondencia. */
@RestController
@RequestMapping("/api/correspondencia")
public class CorrespondenciaController {

    private final CorrespondenciaService correspondencia;

    public CorrespondenciaController(CorrespondenciaService correspondencia) {
        this.correspondencia = correspondencia;
    }

    @SoloVigilante
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CorrespondenciaResponse registrar(@Valid @RequestBody RegistrarCorrespondenciaRequest solicitud) {
        return correspondencia.registrar(solicitud);
    }

    @SoloVigilante
    @PatchMapping("/{id}/entregar")
    public CorrespondenciaResponse entregar(@PathVariable Long id,
                                            @Valid @RequestBody EntregarCorrespondenciaRequest solicitud) {
        return correspondencia.entregar(id, solicitud);
    }

    @SoloVigilante
    @GetMapping
    public List<CorrespondenciaResponse> listar(@RequestParam(required = false) Long personaId) {
        return correspondencia.listar(personaId);
    }

    @SoloPersonaVinculada
    @GetMapping("/mias")
    public List<CorrespondenciaResponse> mias() {
        return correspondencia.mias();
    }
}
