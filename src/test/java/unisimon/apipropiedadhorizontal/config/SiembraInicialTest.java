package unisimon.apipropiedadhorizontal.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** La siembra es idempotente: correrla de nuevo no duplica valores y deja los esperados en cada catalogo. */
@SpringBootTest
class SiembraInicialTest {

    @Autowired CatalogosIniciales catalogosIniciales;
    @Autowired ValorCatalogoRepository catalogos;

    @Test
    void catalogosSembradosNoSeDuplican() {
        catalogosIniciales.run(new DefaultApplicationArguments());
        catalogosIniciales.run(new DefaultApplicationArguments());

        for (ETipoCatalogo tipo : ETipoCatalogo.values()) {
            List<String> nombres = catalogos.findByTipoOrderByNombre(tipo).stream().map(ValorCatalogo::getNombre).toList();
            assertThat(nombres).as(tipo.name()).isNotEmpty().doesNotHaveDuplicates();
        }
        assertThat(catalogos.findByTipoOrderByNombre(ETipoCatalogo.TIPO_INMUEBLE).stream().map(ValorCatalogo::getNombre))
                .contains("Apartamento", "Casa", "Local comercial", "Parqueadero");
        assertThat(catalogos.findByTipoOrderByNombre(ETipoCatalogo.TIPO_PQRS).stream().map(ValorCatalogo::getNombre))
                .contains("Petición", "Queja", "Reclamo", "Sugerencia");
    }
}
