package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionGeneral;
import unisimon.apipropiedadhorizontal.repository.ConfiguracionGeneralRepository;
import unisimon.apipropiedadhorizontal.service.ConfiguracionGeneralService;
import unisimon.apipropiedadhorizontal.web.dto.ConfiguracionGeneralRequest;
import unisimon.apipropiedadhorizontal.web.dto.ConfiguracionGeneralResponse;

@Service
@Transactional
public class ConfiguracionGeneralServiceImpl implements ConfiguracionGeneralService {

    private final ConfiguracionGeneralRepository configuraciones;

    public ConfiguracionGeneralServiceImpl(ConfiguracionGeneralRepository configuraciones) {
        this.configuraciones = configuraciones;
    }

    @Override
    public ConfiguracionGeneralResponse obtener() {
        return ConfiguracionGeneralResponse.de(actual());
    }

    @Override
    public ConfiguracionGeneralResponse actualizar(ConfiguracionGeneralRequest s) {
        ConfiguracionGeneral configuracion = actual();
        configuracion.actualizar(s.maxVehiculosPorInmueble(), s.plazoRespuestaPqrsDias());
        return ConfiguracionGeneralResponse.de(configuracion);
    }

    /** Es una unica fila; si falta (base nueva sin siembra) se crea con los valores por defecto. */
    private ConfiguracionGeneral actual() {
        return configuraciones.findAll().stream().findFirst()
                .orElseGet(() -> configuraciones.save(ConfiguracionGeneral.porDefecto()));
    }
}
