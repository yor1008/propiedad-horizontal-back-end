package unisimon.apipropiedadhorizontal.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloPersonaVinculada;
import unisimon.apipropiedadhorizontal.service.NotificacionService;
import unisimon.apipropiedadhorizontal.web.dto.ConteoNotificacionesResponse;
import unisimon.apipropiedadhorizontal.web.dto.NotificacionResponse;

import java.util.List;

/** CU-004 Consultar notificaciones. */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificaciones;

    public NotificacionController(NotificacionService notificaciones) {
        this.notificaciones = notificaciones;
    }

    @SoloPersonaVinculada
    @GetMapping("/mias")
    public List<NotificacionResponse> mias(@RequestParam(defaultValue = "false") boolean soloNoLeidas) {
        return notificaciones.mias(soloNoLeidas);
    }

    @SoloPersonaVinculada
    @GetMapping("/mias/conteo")
    public ConteoNotificacionesResponse conteo() {
        return notificaciones.conteoNoLeidas();
    }

    @SoloPersonaVinculada
    @PatchMapping("/{id}/leer")
    public NotificacionResponse leer(@PathVariable Long id) {
        return notificaciones.marcarLeida(id);
    }

    @SoloPersonaVinculada
    @PatchMapping("/leer-todas")
    public ConteoNotificacionesResponse leerTodas() {
        return notificaciones.marcarTodasLeidas();
    }
}
