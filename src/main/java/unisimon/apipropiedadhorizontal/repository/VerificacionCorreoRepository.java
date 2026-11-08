package unisimon.apipropiedadhorizontal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unisimon.apipropiedadhorizontal.domain.model.VerificacionCorreo;

import java.util.Optional;

public interface VerificacionCorreoRepository extends JpaRepository<VerificacionCorreo, Long> {

    Optional<VerificacionCorreo> findByCorreo(String correo);
}
