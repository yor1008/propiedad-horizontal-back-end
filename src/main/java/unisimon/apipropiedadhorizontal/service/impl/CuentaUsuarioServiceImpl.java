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

        Persona persona = null;
        if (solicitud.personaId() != null) {
            persona = personas.findById(solicitud.personaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Persona", solicitud.personaId()));
            if (cuentas.existsByPersonaId(persona.getId())) {
                throw new ConflictoException("La persona ya tiene una cuenta de acceso");
            }
        } else if (solicitud.rol() == ERolCuenta.PERSONA_VINCULADA) {
            throw new ReglaNegocioException("Una cuenta de persona vinculada requiere personaId");
        }

        CuentaUsuario cuenta = CuentaUsuario.builder()
                .correoAcceso(correo)
                .hashContrasena(codificador.encode(solicitud.contrasena()))
                .rol(solicitud.rol())
                .persona(persona)
                .build();
        return CuentaResponse.de(cuentas.save(cuenta));
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
