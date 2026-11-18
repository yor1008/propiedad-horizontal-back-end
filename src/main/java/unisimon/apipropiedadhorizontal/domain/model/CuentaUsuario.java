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
 * Credenciales de acceso. La contrasena solo se guarda como hash BCrypt.
 * <p>
 * Restriccion del diagrama de clases (nota sobre {@code CuentaUsuario}):
 * {@code rol = ADMINISTRADOR o VIGILANTE -> persona = null} y
 * {@code rol = PERSONA_VINCULADA -> persona <> null y persona.tipoPersona <> null}.
 * Las cuentas nuevas se crean con {@link #crearCuentaPersonal} o {@link #crearCuentaPersonaVinculada},
 * que la hacen cumplir.
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

    /**
     * Crea la cuenta de un miembro del personal (administrador o vigilante), que no tiene persona.
     *
     * @param contrasena la contrasena <b>ya cifrada</b> con BCrypt: la entidad no conoce el codificador
     *                   (lo aplica el servicio), asi el hash nunca se calcula ni se guarda en claro aqui.
     * @throws IllegalArgumentException si el rol no es de personal
     */
    public static CuentaUsuario crearCuentaPersonal(String correo, String contrasena, ERolCuenta rol) {
        if (!esRolPersonal(rol)) {
            throw new IllegalArgumentException("Una cuenta personal debe ser de administrador o vigilante");
        }
        return CuentaUsuario.builder().correoAcceso(correo).hashContrasena(contrasena).rol(rol).build();
    }

    /**
     * Crea la cuenta de acceso de una persona vinculada (propietario o residente).
     *
     * @param contrasena la contrasena <b>ya cifrada</b> con BCrypt (ver {@link #crearCuentaPersonal}).
     * @throws IllegalArgumentException si falta la persona o su tipo de persona
     */
    public static CuentaUsuario crearCuentaPersonaVinculada(String correo, String contrasena, Persona persona) {
        if (persona == null || persona.getTipoPersona() == null) {
            throw new IllegalArgumentException("Una cuenta de persona vinculada requiere una persona con tipo de persona");
        }
        return CuentaUsuario.builder().correoAcceso(correo).hashContrasena(contrasena)
                .rol(ERolCuenta.PERSONA_VINCULADA).persona(persona).build();
    }

    private static boolean esRolPersonal(ERolCuenta rol) {
        return rol == ERolCuenta.ADMINISTRADOR || rol == ERolCuenta.VIGILANTE;
    }

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
