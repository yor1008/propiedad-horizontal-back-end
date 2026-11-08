package unisimon.apipropiedadhorizontal.web.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.security.anotaciones.SoloAdministrador;
import unisimon.apipropiedadhorizontal.service.ConfiguracionGeneralService;
import unisimon.apipropiedadhorizontal.service.ConfiguracionNotificacionService;
import unisimon.apipropiedadhorizontal.web.dto.ConfiguracionGeneralRequest;
import unisimon.apipropiedadhorizontal.web.dto.ConfiguracionGeneralResponse;
import unisimon.apipropiedadhorizontal.web.dto.ConfiguracionNotificacionRequest;
import unisimon.apipropiedadhorizontal.web.dto.ConfiguracionNotificacionResponse;

import java.util.List;

/** CU-007 Configurar parametros y notificaciones. */
@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionController {

    private final ConfiguracionGeneralService general;
    private final ConfiguracionNotificacionService notificaciones;

    public ConfiguracionController(ConfiguracionGeneralService general, ConfiguracionNotificacionService notificaciones) {
        this.general = general;
        this.notificaciones = notificaciones;
    }

    @SoloAdministrador
    @GetMapping("/general")
    public ConfiguracionGeneralResponse obtenerGeneral() {
        return general.obtener();
    }

    @SoloAdministrador
    @PutMapping("/general")
    public ConfiguracionGeneralResponse actualizarGeneral(@Valid @RequestBody ConfiguracionGeneralRequest solicitud) {
        return general.actualizar(solicitud);
    }

    @SoloAdministrador
    @GetMapping("/notificaciones")
    public List<ConfiguracionNotificacionResponse> listarNotificaciones() {
        return notificaciones.listar();
    }

    @SoloAdministrador
    @PutMapping("/notificaciones/{evento}")
    public ConfiguracionNotificacionResponse actualizarNotificacion(
            @PathVariable ETipoEvento evento, @Valid @RequestBody ConfiguracionNotificacionRequest solicitud) {
        return notificaciones.actualizar(evento, solicitud);
    }
}
