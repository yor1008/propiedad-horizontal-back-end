package unisimon.apipropiedadhorizontal.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloPropietario;
import unisimon.apipropiedadhorizontal.service.MisInmueblesService;
import unisimon.apipropiedadhorizontal.web.dto.MiInmuebleResponse;

import java.util.List;

/** CU-011 Consultar inmuebles propios. */
@RestController
@RequestMapping("/api/mis-inmuebles")
public class MisInmueblesController {

    private final MisInmueblesService misInmuebles;

    public MisInmueblesController(MisInmueblesService misInmuebles) {
        this.misInmuebles = misInmuebles;
    }

    @SoloPropietario
    @GetMapping
    public List<MiInmuebleResponse> consultar() {
        return misInmuebles.consultar();
    }
}
