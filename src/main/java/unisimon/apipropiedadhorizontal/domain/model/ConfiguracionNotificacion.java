package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;

import java.time.LocalDateTime;
import java.util.List;

/** Define si un evento genera notificacion interna y si ademas se envia por correo. */
@Entity
@Table(name = "configuracion_notificacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class ConfiguracionNotificacion extends EntidadBase {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 40)
    private ETipoEvento evento;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;

    @Column(name = "enviar_correo", nullable = false)
    private boolean enviarCorreo;

    public void configurar(boolean activa, boolean enviarCorreo) {
        this.activa = activa;
        this.enviarCorreo = enviarCorreo;
    }

    /** Crea una notificacion por destinatario; lista vacia si el evento esta desactivado. */
    public List<Notificacion> generar(List<Persona> destinatarios, String mensaje) {
        if (!activa) {
            return List.of();
        }
        LocalDateTime ahora = LocalDateTime.now();
        return destinatarios.stream()
                .<Notificacion>map(persona -> Notificacion.builder()
                        .persona(persona)
                        .evento(evento)
                        .mensaje(mensaje)
                        .fechaHora(ahora)
                        .enviadaPorCorreo(enviarCorreo)
                        .build())
                .toList();
    }
}
