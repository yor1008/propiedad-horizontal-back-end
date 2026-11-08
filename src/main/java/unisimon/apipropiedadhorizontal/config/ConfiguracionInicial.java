package unisimon.apipropiedadhorizontal.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionGeneral;
import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionNotificacion;
import unisimon.apipropiedadhorizontal.repository.ConfiguracionGeneralRepository;
import unisimon.apipropiedadhorizontal.repository.ConfiguracionNotificacionRepository;

/**
 * Siembra (CU-007) la configuracion general y una fila por evento de notificacion. Solo crea lo que falta: si el
 * administrador ya cambio los valores, un reinicio no los pisa.
 */
@Component
public class ConfiguracionInicial implements ApplicationRunner {

    private final ConfiguracionGeneralRepository general;
    private final ConfiguracionNotificacionRepository notificaciones;

    public ConfiguracionInicial(ConfiguracionGeneralRepository general, ConfiguracionNotificacionRepository notificaciones) {
        this.general = general;
        this.notificaciones = notificaciones;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (general.count() == 0) {
            general.save(ConfiguracionGeneral.porDefecto());
        }
        for (ETipoEvento evento : ETipoEvento.values()) {
            if (notificaciones.findByEvento(evento).isEmpty()) {
                notificaciones.save(ConfiguracionNotificacion.builder().evento(evento).build());
            }
        }
    }
}
