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
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloResidente;
import unisimon.apipropiedadhorizontal.service.PreRegistroService;
import unisimon.apipropiedadhorizontal.web.dto.CrearPreRegistroRequest;
import unisimon.apipropiedadhorizontal.web.dto.PreRegistroResponse;

import java.util.List;

/** CU-015 Gestionar pre-registros de visitantes. */
@RestController
@RequestMapping("/api/pre-registros")
public class PreRegistroController {

    private final PreRegistroService preRegistros;

    public PreRegistroController(PreRegistroService preRegistros) {
        this.preRegistros = preRegistros;
    }

    @SoloResidente
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PreRegistroResponse crear(@Valid @RequestBody CrearPreRegistroRequest solicitud) {
        return preRegistros.crear(solicitud);
    }

    @SoloResidente
    @GetMapping("/mios")
    public List<PreRegistroResponse> mios() {
        return preRegistros.mios();
    }

    @SoloResidente
    @PatchMapping("/{id}/cancelar")
    public PreRegistroResponse cancelar(@PathVariable Long id) {
        return preRegistros.cancelar(id);
    }
}
