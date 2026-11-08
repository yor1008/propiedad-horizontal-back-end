package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.service.CatalogoService;
import unisimon.apipropiedadhorizontal.web.dto.CatalogoRequest;
import unisimon.apipropiedadhorizontal.web.dto.CatalogoResponse;

import java.util.List;

@Service
@Transactional
public class CatalogoServiceImpl implements CatalogoService {

    private final ValorCatalogoRepository catalogos;

    public CatalogoServiceImpl(ValorCatalogoRepository catalogos) {
        this.catalogos = catalogos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoResponse> listar(ETipoCatalogo tipo, boolean soloActivos) {
        List<ValorCatalogo> valores = soloActivos
                ? catalogos.findByTipoAndActivoTrueOrderByNombre(tipo)
                : catalogos.findByTipoOrderByNombre(tipo);
        return valores.stream().map(CatalogoResponse::de).toList();
    }

    @Override
    public CatalogoResponse crear(CatalogoRequest solicitud) {
        String nombre = solicitud.nombre().trim();
        if (catalogos.existsByTipoAndNombreIgnoreCase(solicitud.tipo(), nombre)) {
            throw new ConflictoException("Ya existe un valor '" + nombre + "' en " + solicitud.tipo());
        }
        ValorCatalogo valor = ValorCatalogo.builder().tipo(solicitud.tipo()).nombre(nombre).build();
        return CatalogoResponse.de(catalogos.save(valor));
    }

    @Override
    public CatalogoResponse renombrar(Long id, String nombre) {
        ValorCatalogo valor = buscar(id);
        String limpio = nombre.trim();
        if (!valor.getNombre().equalsIgnoreCase(limpio)
                && catalogos.existsByTipoAndNombreIgnoreCase(valor.getTipo(), limpio)) {
            throw new ConflictoException("Ya existe un valor '" + limpio + "' en " + valor.getTipo());
        }
        valor.modificar(limpio);
        return CatalogoResponse.de(valor);
    }

    @Override
    public CatalogoResponse desactivar(Long id) {
        ValorCatalogo valor = buscar(id);
        valor.desactivar();
        return CatalogoResponse.de(valor);
    }

    @Override
    public CatalogoResponse reactivar(Long id) {
        ValorCatalogo valor = buscar(id);
        valor.reactivar();
        return CatalogoResponse.de(valor);
    }

    private ValorCatalogo buscar(Long id) {
        return catalogos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Valor de catalogo", id));
    }

    @Override
    public void eliminar(Long id) {
        Borrado.fisico(catalogos, buscar(id),
                "El valor de catalogo esta en uso (personas, inmuebles, vehiculos, PQRS...) y no se puede borrar; desactivelo en su lugar");
    }
}
