package unisimon.apipropiedadhorizontal.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unisimon.apipropiedadhorizontal.security.anotaciones.Autenticado;
import unisimon.apipropiedadhorizontal.service.TipoPersonaService;
import unisimon.apipropiedadhorizontal.web.dto.TipoPersonaResponse;

import java.util.List;

/** Lectura de tipos de persona, mismo patron que los catalogos (cualquier usuario autenticado). */
@RestController
@RequestMapping("/api/tipos-persona")
public class TipoPersonaController {

    private final TipoPersonaService tipos;

    public TipoPersonaController(TipoPersonaService tipos) {
        this.tipos = tipos;
    }

    @Autenticado
    @GetMapping
    public List<TipoPersonaResponse> listar() {
        return tipos.listarActivos();
    }
}
