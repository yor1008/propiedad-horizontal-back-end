package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ECategoriaInmueble;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.model.Inmueble;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.InmuebleRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.service.ParqueaderoService;
import unisimon.apipropiedadhorizontal.web.dto.CrearParqueaderoRequest;
import unisimon.apipropiedadhorizontal.web.dto.ParqueaderoResponse;

import java.util.List;

@Service
@Transactional
public class ParqueaderoServiceImpl implements ParqueaderoService {

    private final InmuebleRepository inmuebles;
    private final ValorCatalogoRepository catalogos;

    public ParqueaderoServiceImpl(InmuebleRepository inmuebles, ValorCatalogoRepository catalogos) {
        this.inmuebles = inmuebles;
        this.catalogos = catalogos;
    }

    @Override
    public ParqueaderoResponse crear(CrearParqueaderoRequest s) {
        String torre = s.torreBloque().trim();
        String numero = s.numero().trim();
        if (inmuebles.existsByTorreBloqueAndNumero(torre, numero)) {
            throw new ConflictoException("Ya existe el inmueble " + torre + "-" + numero);
        }
        ValorCatalogo tipo = catalogos.findById(s.tipoId())
                .filter(v -> v.getTipo() == ETipoCatalogo.TIPO_INMUEBLE)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de inmueble", s.tipoId()));
        if (!tipo.isActivo()) {
            throw new ReglaNegocioException("El tipo de inmueble esta inactivo");
        }
        return ParqueaderoResponse.de(inmuebles.save(Inmueble.builder()
                .torreBloque(torre).numero(numero).piso(s.piso()).tipo(tipo)
                .categoria(ECategoriaInmueble.PARQUEADERO).build()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParqueaderoResponse> listar() {
        return inmuebles.findByCategoria(ECategoriaInmueble.PARQUEADERO).stream()
                .map(ParqueaderoResponse::de).toList();
    }

    @Override
    public ParqueaderoResponse asignar(Long parqueaderoId, Long inmuebleId) {
        Inmueble parqueadero = buscarParqueadero(parqueaderoId);
        if (!parqueadero.isActivo()) {
            throw new ReglaNegocioException("El parqueadero esta inactivo");
        }
        if (parqueadero.getInmuebleAsociado() != null) {
            throw new ConflictoException("El parqueadero ya esta asignado; liberelo primero");
        }
        Inmueble inmueble = inmuebles.findById(inmuebleId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inmueble", inmuebleId));
        if (inmueble.getCategoria() != ECategoriaInmueble.UNIDAD_PRIVADA || !inmueble.isActivo()) {
            throw new ReglaNegocioException("Solo se asigna a una unidad privada activa");
        }
        inmueble.asignarParqueadero(parqueadero);
        return ParqueaderoResponse.de(parqueadero);
    }

    @Override
    public ParqueaderoResponse liberar(Long parqueaderoId) {
        Inmueble parqueadero = buscarParqueadero(parqueaderoId);
        Inmueble duenio = parqueadero.getInmuebleAsociado();
        if (duenio == null) {
            throw new ReglaNegocioException("El parqueadero no esta asignado");
        }
        duenio.retirarParqueadero(parqueadero);
        return ParqueaderoResponse.de(parqueadero);
    }

    @Override
    public ParqueaderoResponse desactivar(Long parqueaderoId, String motivo) {
        Inmueble parqueadero = buscarParqueadero(parqueaderoId);
        parqueadero.desactivar(motivo.trim());
        return ParqueaderoResponse.de(parqueadero);
    }

    private Inmueble buscarParqueadero(Long id) {
        return inmuebles.findById(id)
                .filter(i -> i.getCategoria() == ECategoriaInmueble.PARQUEADERO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Parqueadero", id));
    }

    @Override
    public void eliminar(Long id) {
        Inmueble parqueadero = buscarParqueadero(id);
        if (parqueadero.getInmuebleAsociado() != null) {
            throw new ReglaNegocioException("El parqueadero esta asignado a un inmueble; liberelo antes de borrarlo");
        }
        Borrado.fisico(inmuebles, parqueadero,
                "El parqueadero tiene visitas registradas y no se puede borrar; desactivelo en su lugar");
    }
}
