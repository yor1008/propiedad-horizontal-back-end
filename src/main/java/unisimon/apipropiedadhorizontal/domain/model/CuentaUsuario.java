package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;

import java.time.LocalDateTime;

/**
 * Credenciales de acceso. Administradores y vigilantes pueden no tener {@link Persona};
 * las cuentas PERSONA_VINCULADA siempre la tienen (regla validada en el servicio).
 * La contrasena solo se guarda como hash BCrypt.
 */
@Entity
@Table(name = "cuenta_usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class CuentaUsuario extends EntidadBase {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", unique = true, foreignKey = @ForeignKey(name = "fk_cuenta_persona"))
    private Persona persona;

    @Column(name = "correo_acceso", nullable = false, unique = true, length = 150)
    private String correoAcceso;

    @Column(name = "hash_contrasena", nullable = false, length = 100)
    private String hashContrasena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ERolCuenta rol;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;

    @Column(name = "fecha_ultimo_acceso")
    private LocalDateTime fechaUltimoAcceso;

    public void actualizarHashContrasena(String nuevoHash) {
        this.hashContrasena = nuevoHash;
    }

    public void registrarAcceso(LocalDateTime momento) {
        this.fechaUltimoAcceso = momento;
    }

    public void desactivar() {
        this.activa = false;
    }

    public void reactivar() {
        this.activa = true;
    }
}
