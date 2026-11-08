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
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloResidente;
import unisimon.apipropiedadhorizontal.service.GestionReservaService;
import unisimon.apipropiedadhorizontal.service.ReservaService;
import unisimon.apipropiedadhorizontal.web.dto.AprobarReservaRequest;
import unisimon.apipropiedadhorizontal.web.dto.MotivoRequest;
import unisimon.apipropiedadhorizontal.web.dto.ReservaResponse;
import unisimon.apipropiedadhorizontal.web.dto.ReservarRequest;

import java.util.List;

/** CU-025/026 (residente) y CU-027 (administrador). */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservas;
    private final GestionReservaService gestion;

    public ReservaController(ReservaService reservas, GestionReservaService gestion) {
        this.reservas = reservas;
        this.gestion = gestion;
    }

    @SoloResidente
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse reservar(@Valid @RequestBody ReservarRequest solicitud) {
        return reservas.reservar(solicitud);
    }

    @SoloResidente
    @GetMapping("/mias")
    public List<ReservaResponse> mias() {
        return reservas.mias();
    }

    @SoloResidente
    @PatchMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Long id, @Valid @RequestBody MotivoRequest solicitud) {
        return reservas.cancelar(id, solicitud.motivo());
    }

    @SoloAdministrador
    @GetMapping
    public List<ReservaResponse> listar() {
        return gestion.listar();
    }

    @SoloAdministrador
    @PatchMapping("/{id}/aprobar")
    public ReservaResponse aprobar(@PathVariable Long id,
                                   @Valid @RequestBody(required = false) AprobarReservaRequest solicitud) {
        return gestion.aprobar(id, solicitud == null ? null : solicitud.condiciones());
    }

    @SoloAdministrador
    @PatchMapping("/{id}/rechazar")
    public ReservaResponse rechazar(@PathVariable Long id, @Valid @RequestBody MotivoRequest solicitud) {
        return gestion.rechazar(id, solicitud.motivo());
    }

    @SoloAdministrador
    @PatchMapping("/{id}/anular")
    public ReservaResponse anular(@PathVariable Long id, @Valid @RequestBody MotivoRequest solicitud) {
        return gestion.anular(id, solicitud.motivo());
    }

    @SoloAdministrador
    @PatchMapping("/{id}/finalizar")
    public ReservaResponse finalizar(@PathVariable Long id) {
        return gestion.finalizar(id);
    }
}
