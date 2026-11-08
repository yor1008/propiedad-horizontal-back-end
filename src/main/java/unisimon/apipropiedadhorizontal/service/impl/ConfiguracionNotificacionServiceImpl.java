package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionNotificacion;
import unisimon.apipropiedadhorizontal.repository.ConfiguracionNotificacionRepository;
import unisimon.apipropiedadhorizontal.service.ConfiguracionNotificacionService;
import unisimon.apipropiedadhorizontal.web.dto.ConfiguracionNotificacionRequest;
import unisimon.apipropiedadhorizontal.web.dto.ConfiguracionNotificacionResponse;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class ConfiguracionNotificacionServiceImpl implements ConfiguracionNotificacionService {

    private final ConfiguracionNotificacionRepository configuraciones;

    public ConfiguracionNotificacionServiceImpl(ConfiguracionNotificacionRepository configuraciones) {
        this.configuraciones = configuraciones;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionNotificacionResponse> listar() {
        return Arrays.stream(ETipoEvento.values())
                .map(evento -> configuraciones.findByEvento(evento)
                        .orElseGet(() -> ConfiguracionNotificacion.builder().evento(evento).build()))
                .map(ConfiguracionNotificacionResponse::de)
                .toList();
    }

    @Override
    public ConfiguracionNotificacionResponse actualizar(ETipoEvento evento, ConfiguracionNotificacionRequest s) {
        ConfiguracionNotificacion configuracion = configuraciones.findByEvento(evento)
                .orElseGet(() -> configuraciones.save(ConfiguracionNotificacion.builder().evento(evento).build()));
        configuracion.configurar(s.activa(), s.enviarCorreo());
        return ConfiguracionNotificacionResponse.de(configuracion);
    }
}
