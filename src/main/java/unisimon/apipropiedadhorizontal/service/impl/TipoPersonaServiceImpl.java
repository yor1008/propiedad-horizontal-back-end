package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.repository.TipoPersonaRepository;
import unisimon.apipropiedadhorizontal.service.TipoPersonaService;
import unisimon.apipropiedadhorizontal.web.dto.TipoPersonaResponse;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TipoPersonaServiceImpl implements TipoPersonaService {

    private final TipoPersonaRepository tipos;

    public TipoPersonaServiceImpl(TipoPersonaRepository tipos) {
        this.tipos = tipos;
    }

    @Override
    public List<TipoPersonaResponse> listarActivos() {
        return tipos.findByActivoTrueOrderByNombre().stream().map(TipoPersonaResponse::de).toList();
    }
}
