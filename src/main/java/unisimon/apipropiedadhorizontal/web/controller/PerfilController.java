package unisimon.apipropiedadhorizontal.web.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloPersonaVinculada;
import unisimon.apipropiedadhorizontal.service.PerfilService;
import unisimon.apipropiedadhorizontal.web.dto.ActualizarPersonaRequest;
import unisimon.apipropiedadhorizontal.web.dto.PersonaResponse;

/** CU-003 Gestionar datos propios. */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService perfil;

    public PerfilController(PerfilService perfil) {
        this.perfil = perfil;
    }

    @SoloPersonaVinculada
    @GetMapping
    public PersonaResponse obtener() {
        return perfil.obtener();
    }

    @SoloPersonaVinculada
    @PutMapping
    public PersonaResponse actualizar(@Valid @RequestBody ActualizarPersonaRequest solicitud) {
        return perfil.actualizar(solicitud);
    }
}
