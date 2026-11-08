package unisimon.apipropiedadhorizontal.support;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import unisimon.apipropiedadhorizontal.service.EnviadorCorreo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Sustituye al SMTP en las pruebas: guarda los correos en memoria para poder comprobarlos (y leer el enlace). */
@Primary
@Component
public class CorreoCapturado implements EnviadorCorreo {

    public record Mensaje(String destinatario, String asunto, String texto) {
    }

    private final List<Mensaje> enviados = new CopyOnWriteArrayList<>();

    @Override
    public void enviar(String destinatario, String asunto, String texto) {
        enviados.add(new Mensaje(destinatario, asunto, texto));
    }

    public List<Mensaje> enviados() {
        return List.copyOf(enviados);
    }

    public void limpiar() {
        enviados.clear();
    }
}
