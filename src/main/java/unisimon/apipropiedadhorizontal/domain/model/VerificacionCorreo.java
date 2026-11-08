package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Codigo de verificacion pendiente para un correo (se exige al crear una persona). Solo se guarda la huella del
 * codigo, nunca el codigo. Hay un registro por correo: pedir otro codigo reemplaza al anterior.
 */
@Entity
@Table(name = "verificacion_correo",
        uniqueConstraints = @UniqueConstraint(name = "uk_verificacion_correo_correo", columnNames = "correo"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class VerificacionCorreo extends EntidadBase {

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(name = "hash_codigo", nullable = false, length = 64)
    private String hashCodigo;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(nullable = false)
    private int intentos;

    /** Un codigo nuevo invalida al anterior y reinicia los intentos. */
    public void renovar(String hashCodigo, LocalDateTime expiraEn) {
        this.hashCodigo = hashCodigo;
        this.expiraEn = expiraEn;
        this.intentos = 0;
    }

    public void registrarIntentoFallido() {
        this.intentos++;
    }

    public boolean estaVencido(LocalDateTime ahora) {
        return !ahora.isBefore(expiraEn);
    }
}
