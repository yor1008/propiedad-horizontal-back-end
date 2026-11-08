package unisimon.apipropiedadhorizontal.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoEvento;
import unisimon.apipropiedadhorizontal.domain.model.ConfiguracionNotificacion;
import unisimon.apipropiedadhorizontal.domain.model.Persona;
import unisimon.apipropiedadhorizontal.domain.model.TipoPersona;
import unisimon.apipropiedadhorizontal.domain.model.ValorCatalogo;
import unisimon.apipropiedadhorizontal.repository.ConfiguracionNotificacionRepository;
import unisimon.apipropiedadhorizontal.repository.NotificacionRepository;
import unisimon.apipropiedadhorizontal.repository.PersonaRepository;
import unisimon.apipropiedadhorizontal.repository.TipoPersonaRepository;
import unisimon.apipropiedadhorizontal.repository.ValorCatalogoRepository;
import unisimon.apipropiedadhorizontal.service.Notificador;
import unisimon.apipropiedadhorizontal.support.CorreoCapturado;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Correo de las notificaciones: sale solo si el evento lo pide, la persona tiene correo y la transaccion se confirma. */
@SpringBootTest
class CorreoNotificacionesIntegracionTest {

    @Autowired Notificador notificador;
    @Autowired NotificacionRepository notificaciones;
    @Autowired ConfiguracionNotificacionRepository configuraciones;
    @Autowired PersonaRepository personas;
    @Autowired TipoPersonaRepository tiposPersona;
    @Autowired ValorCatalogoRepository catalogos;
    @Autowired PlatformTransactionManager transacciones;
    @Autowired CorreoCapturado correo;

    Persona conCorreo;
    Persona sinCorreo;

    @BeforeEach
    void preparar() {
        limpiar();
        ValorCatalogo documento = catalogos.save(ValorCatalogo.builder().tipo(ETipoCatalogo.TIPO_DOCUMENTO).nombre("Cedula").build());
        TipoPersona tipo = tiposPersona.save(TipoPersona.builder().nombre("Residente").esResidente(true).build());
        conCorreo = persona(tipo, documento, "1001", "ana@ph.com");
        sinCorreo = persona(tipo, documento, "1002", null);
    }

    @AfterEach
    void limpiar() {
        notificaciones.deleteAll();
        configuraciones.deleteAll();
        personas.deleteAll();
        tiposPersona.deleteAll();
        catalogos.deleteAll();
        correo.limpiar();
    }

    @Test
    void sinEnviarCorreoSoloSeCreaLaNotificacion() {
        notificador.notificar(ETipoEvento.INGRESO_VISITANTE, List.of(conCorreo), "Hola");
        assertThat(notificaciones.count()).isEqualTo(1);
        assertThat(notificaciones.findAll().getFirst().isEnviadaPorCorreo()).isFalse();
        assertThat(correo.enviados()).isEmpty();
    }

    @Test
    void conEnviarCorreoSaleUnCorreoSoloAQuienTieneCorreo() {
        configurar(ETipoEvento.INGRESO_VISITANTE, true, true);
        notificador.notificar(ETipoEvento.INGRESO_VISITANTE, List.of(conCorreo, sinCorreo), "Ingreso su visitante");
        assertThat(notificaciones.count()).isEqualTo(2);
        assertThat(correo.enviados()).hasSize(1);
        assertThat(correo.enviados().getFirst().destinatario()).isEqualTo("ana@ph.com");
        assertThat(correo.enviados().getFirst().texto()).isEqualTo("Ingreso su visitante");
        assertThat(notificaciones.findAll()).filteredOn(n -> n.isEnviadaPorCorreo()).hasSize(1);
    }

    @Test
    void eventoDesactivadoNoCreaNadaNiEnviaCorreo() {
        configurar(ETipoEvento.INGRESO_VISITANTE, false, true);
        notificador.notificar(ETipoEvento.INGRESO_VISITANTE, List.of(conCorreo), "Hola");
        assertThat(notificaciones.count()).isZero();
        assertThat(correo.enviados()).isEmpty();
    }

    @Test
    void siLaTransaccionSeRevierteNoSaleCorreo() {
        configurar(ETipoEvento.INGRESO_VISITANTE, true, true);
        try {
            new TransactionTemplate(transacciones).executeWithoutResult(s -> {
                notificador.notificar(ETipoEvento.INGRESO_VISITANTE, List.of(conCorreo), "Hola");
                throw new IllegalStateException("falla posterior en el caso de uso");
            });
        } catch (IllegalStateException esperado) {
            // la transaccion se revierte
        }
        assertThat(notificaciones.count()).isZero();
        assertThat(correo.enviados()).isEmpty();
    }

    // ---------- utilidades ----------

    private void configurar(ETipoEvento evento, boolean activa, boolean enviarCorreo) {
        ConfiguracionNotificacion c = ConfiguracionNotificacion.builder().evento(evento).build();
        c.configurar(activa, enviarCorreo);
        configuraciones.save(c);
    }

    private Persona persona(TipoPersona tipo, ValorCatalogo documento, String numero, String correoPersona) {
        return personas.save(Persona.builder().tipoPersona(tipo).tipoDocumento(documento).numeroDocumento(numero)
                .nombres("Ana").apellidos("Prueba").correo(correoPersona).build());
    }
}
