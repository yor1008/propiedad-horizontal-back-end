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
import unisimon.apipropiedadhorizontal.service.InmuebleService;
import unisimon.apipropiedadhorizontal.web.dto.CrearInmuebleRequest;
import unisimon.apipropiedadhorizontal.web.dto.MotivoRequest;
import unisimon.apipropiedadhorizontal.web.dto.InmuebleResponse;
import unisimon.apipropiedadhorizontal.web.dto.PersonaResponse;
import unisimon.apipropiedadhorizontal.web.dto.VincularPersonaRequest;

import java.util.List;

/** CU-009 Gestionar inmuebles y CU-010 Gestionar vinculaciones. */
@RestController
@RequestMapping("/api")
public class InmuebleController {

    private final InmuebleService inmuebles;

    public InmuebleController(InmuebleService inmuebles) {
        this.inmuebles = inmuebles;
    }

    @SoloAdministrador
    @PostMapping("/inmuebles")
    @ResponseStatus(HttpStatus.CREATED)
    public InmuebleResponse crear(@Valid @RequestBody CrearInmuebleRequest solicitud) {
        return inmuebles.crear(solicitud);
    }

    @SoloAdministrador
    @GetMapping("/inmuebles")
    public List<InmuebleResponse> listar() {
        return inmuebles.listar();
    }

    @SoloAdministrador
    @GetMapping("/inmuebles/{id}")
    public InmuebleResponse obtener(@PathVariable Long id) {
        return inmuebles.obtener(id);
    }

    @SoloAdministrador
    @PutMapping("/inmuebles/{id}")
    public InmuebleResponse actualizar(@PathVariable Long id, @Valid @RequestBody CrearInmuebleRequest solicitud) {
        return inmuebles.actualizar(id, solicitud);
    }

    @SoloAdministrador
    @PatchMapping("/inmuebles/{id}/desactivar")
    public InmuebleResponse desactivar(@PathVariable Long id, @Valid @RequestBody MotivoRequest solicitud) {
        return inmuebles.desactivar(id, solicitud.motivo());
    }

    @SoloAdministrador
    @PatchMapping("/inmuebles/{id}/reactivar")
    public InmuebleResponse reactivar(@PathVariable Long id) {
        return inmuebles.reactivar(id);
    }

    @SoloAdministrador
    @DeleteMapping("/inmuebles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        inmuebles.eliminar(id);
    }

    @SoloAdministrador
    @GetMapping("/inmuebles/{id}/personas")
    public List<PersonaResponse> personas(@PathVariable Long id) {
        return inmuebles.personasDe(id);
    }

    @SoloAdministrador
    @PostMapping("/inmuebles/{id}/vinculos")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonaResponse vincular(@PathVariable Long id, @Valid @RequestBody VincularPersonaRequest solicitud) {
        return inmuebles.vincular(id, solicitud);
    }

    @SoloAdministrador
    @PatchMapping("/personas/{personaId}/finalizar-vinculo")
    public PersonaResponse finalizarVinculo(@PathVariable Long personaId,
                                            @Valid @RequestBody MotivoRequest solicitud) {
        return inmuebles.finalizarVinculo(personaId, solicitud);
    }
}
