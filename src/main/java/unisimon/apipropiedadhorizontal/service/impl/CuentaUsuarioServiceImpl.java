package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.exception.ConflictoException;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.CuentaUsuarioRepository;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.security.UsuarioActual;
import unisimon.apipropiedadhorizontal.service.CuentaUsuarioService;
import unisimon.apipropiedadhorizontal.util.Correos;
import unisimon.apipropiedadhorizontal.web.dto.CrearCuentaRequest;
import unisimon.apipropiedadhorizontal.web.dto.CuentaResponse;

import java.util.List;

@Service
@Transactional
public class CuentaUsuarioServiceImpl implements CuentaUsuarioService {

    private final CuentaUsuarioRepository cuentas;
    private final PersonaRepository personas;
    private final PasswordEncoder codificador;
    private final UsuarioActual usuarioActual;

    public CuentaUsuarioServiceImpl(CuentaUsuarioRepository cuentas, PersonaRepository personas,
                                    PasswordEncoder codificador, UsuarioActual usuarioActual) {
        this.cuentas = cuentas;
        this.personas = personas;
        this.codificador = codificador;
        this.usuarioActual = usuarioActual;
    }

    @Override
    public CuentaResponse crear(CrearCuentaRequest solicitud) {
        String correo = Correos.normalizar(solicitud.correo());
        if (cuentas.existsByCorreoAcceso(correo)) {
            throw new ConflictoException("Ya existe una cuenta con ese correo");
        }

        String hash = codificador.encode(solicitud.contrasena());
        if (solicitud.rol() != ERolCuenta.PERSONA_VINCULADA) {
            // Nota del diagrama: rol = ADMINISTRADOR o VIGILANTE -> persona = null.
            if (solicitud.personaId() != null) {
                throw new ReglaNegocioException("Una cuenta de administrador o vigilante no puede tener persona");
            }
            return CuentaResponse.de(cuentas.save(CuentaUsuario.crearCuentaPersonal(correo, hash, solicitud.rol())));
        }

        // Nota del diagrama: rol = PERSONA_VINCULADA -> persona <> null y persona.tipoPersona <> null.
        if (solicitud.personaId() == null) {
            throw new ReglaNegocioException("Una cuenta de persona vinculada requiere personaId");
        }
        Persona persona = personas.findById(solicitud.personaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona", solicitud.personaId()));
        if (cuentas.existsByPersonaId(persona.getId())) {
            throw new ConflictoException("La persona ya tiene una cuenta de acceso");
        }
        if (persona.getTipoPersona() == null) {
            throw new ReglaNegocioException("La persona no tiene tipo de persona");
        }
        return CuentaResponse.de(cuentas.save(CuentaUsuario.crearCuentaPersonaVinculada(correo, hash, persona)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> listar() {
        return cuentas.findAll().stream().map(CuentaResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaResponse obtener(Long id) {
        return CuentaResponse.de(buscar(id));
    }

    @Override
    public CuentaResponse desactivar(Long id) {
        if (id.equals(usuarioActual.cuentaId())) {
            throw new ReglaNegocioException("No puede desactivar su propia cuenta");
        }
        CuentaUsuario cuenta = buscar(id);
        cuenta.desactivar();
        return CuentaResponse.de(cuenta);
    }

    @Override
    public CuentaResponse reactivar(Long id) {
        CuentaUsuario cuenta = buscar(id);
        cuenta.reactivar();
        return CuentaResponse.de(cuenta);
    }

    private CuentaUsuario buscar(Long id) {
        return cuentas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta", id));
    }

    @Override
    public void eliminar(Long id) {
        CuentaUsuario cuenta = buscar(id);
        if (cuenta.getId().equals(usuarioActual.cuentaId())) {
            throw new ReglaNegocioException("No puede borrar su propia cuenta");
        }
        cuentas.delete(cuenta);
    }
}
