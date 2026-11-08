package unisimon.apipropiedadhorizontal.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.domain.model.CuentaUsuario;

/** Acceso a cuentas. El correo se normaliza a minusculas antes de consultar. */
public interface CuentaUsuarioRepository extends JpaRepository<CuentaUsuario, Long> {

    Optional<CuentaUsuario> findByCorreoAcceso(String correoAcceso);
    boolean existsByCorreoAcceso(String correoAcceso);
    boolean existsByPersonaId(Long personaId);
    boolean existsByRol(ERolCuenta rol);
}
