package unisimon.apipropiedadhorizontal.web.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import unisimon.apipropiedadhorizontal.service.PersonaService;
import unisimon.apipropiedadhorizontal.web.dto.ActualizarPersonaRequest;
import unisimon.apipropiedadhorizontal.web.dto.CrearPersonaRequest;
import unisimon.apipropiedadhorizontal.web.dto.MotivoRequest;
import unisimon.apipropiedadhorizontal.web.dto.PersonaResponse;

import java.util.List;

/** CU-008 Gestionar personas. */
@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private final PersonaService personas;

    public PersonaController(PersonaService personas) {
        this.personas = personas;
    }

    @SoloAdministrador
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonaResponse crear(@Valid @RequestBody CrearPersonaRequest solicitud) {
        return personas.crear(solicitud);
    }

    @SoloAdministrador
    @GetMapping
    public List<PersonaResponse> listar() {
        return personas.listar();
    }

    @SoloAdministrador
    @GetMapping("/{id}")
    public PersonaResponse obtener(@PathVariable Long id) {
        return personas.obtener(id);
    }

    @SoloAdministrador
    @PutMapping("/{id}")
    public PersonaResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarPersonaRequest solicitud) {
        return personas.actualizar(id, solicitud);
    }

    @SoloAdministrador
    @PatchMapping("/{id}/desactivar")
    public PersonaResponse desactivar(@PathVariable Long id, @Valid @RequestBody MotivoRequest solicitud) {
        return personas.desactivar(id, solicitud.motivo());
    }

    @SoloAdministrador
    @PatchMapping("/{id}/reactivar")
    public PersonaResponse reactivar(@PathVariable Long id) {
        return personas.reactivar(id);
    }

    @SoloAdministrador
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        personas.eliminar(id);
    }
}
