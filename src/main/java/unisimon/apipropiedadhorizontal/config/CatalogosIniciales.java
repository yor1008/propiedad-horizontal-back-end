package unisimon.apipropiedadhorizontal.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;

import java.util.List;
import java.util.Map;

import static unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo.*;

/**
 * Siembra valores iniciales de los catalogos (CU-006) para poder crear personas, inmuebles, vehiculos, etc. desde el
 * primer arranque. Solo agrega los que faltan por nombre: el administrador puede renombrarlos o desactivarlos, y un
 * valor desactivado no se vuelve a crear ni a activar.
 */
@Component
public class CatalogosIniciales implements ApplicationRunner {

    private static final Map<ETipoCatalogo, List<String>> VALORES = Map.of(
            TIPO_INMUEBLE, List.of("Apartamento", "Casa", "Local comercial", "Parqueadero"),
            TIPO_DOCUMENTO, List.of("Cédula de ciudadanía", "Cédula de extranjería", "Pasaporte", "NIT"),
            TIPO_VEHICULO, List.of("Carro", "Moto", "Bicicleta"),
            TIPO_CORRESPONDENCIA, List.of("Paquete", "Carta", "Documento"),
            TIPO_PQRS, List.of("Petición", "Queja", "Reclamo", "Sugerencia"),
            TIPO_MANTENIMIENTO, List.of("Preventivo", "Correctivo"));

    private final ValorCatalogoRepository catalogos;

    public CatalogosIniciales(ValorCatalogoRepository catalogos) {
        this.catalogos = catalogos;
    }

    @Override
    public void run(ApplicationArguments args) {
        VALORES.forEach((tipo, nombres) -> nombres.forEach(nombre -> {
            if (!catalogos.existsByTipoAndNombreIgnoreCase(tipo, nombre)) {
                catalogos.save(ValorCatalogo.builder().tipo(tipo).nombre(nombre).build());
            }
        }));
    }
}
