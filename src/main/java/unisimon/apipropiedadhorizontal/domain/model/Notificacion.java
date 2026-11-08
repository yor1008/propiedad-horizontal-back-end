package unisimon.apipropiedadhorizontal.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion", indexes = @Index(name = "ix_notificacion_persona", columnList = "persona_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Notificacion extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notificacion_persona"))
    private Persona persona;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ETipoEvento evento;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private boolean leida;

    @Column(name = "enviada_por_correo", nullable = false)
    private boolean enviadaPorCorreo;

    public void marcarLeida() {
        this.leida = true;
    }
}
