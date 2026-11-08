package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.Notificacion;
import unisimon.apipropiedadhorizontal.exception.RecursoNoEncontradoException;
import unisimon.apipropiedadhorizontal.repository.NotificacionRepository;
import unisimon.apipropiedadhorizontal.service.NotificacionService;
import unisimon.apipropiedadhorizontal.service.PersonaAutenticada;
import unisimon.apipropiedadhorizontal.web.dto.ConteoNotificacionesResponse;
import unisimon.apipropiedadhorizontal.web.dto.NotificacionResponse;

import java.util.List;

@Service
@Transactional
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificaciones;
    private final PersonaAutenticada personaAutenticada;

    public NotificacionServiceImpl(NotificacionRepository notificaciones, PersonaAutenticada personaAutenticada) {
        this.notificaciones = notificaciones;
        this.personaAutenticada = personaAutenticada;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionResponse> mias(boolean soloNoLeidas) {
        Long personaId = personaAutenticada.id();
        List<Notificacion> lista = soloNoLeidas
                ? notificaciones.findByPersonaIdAndLeidaFalseOrderByFechaHoraDesc(personaId)
                : notificaciones.findByPersonaIdOrderByFechaHoraDesc(personaId);
        return lista.stream().map(NotificacionResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConteoNotificacionesResponse conteoNoLeidas() {
        return new ConteoNotificacionesResponse(notificaciones.countByPersonaIdAndLeidaFalse(personaAutenticada.id()));
    }

    @Override
    public NotificacionResponse marcarLeida(Long id) {
        Long personaId = personaAutenticada.id();
        Notificacion n = notificaciones.findById(id)
                .filter(e -> e.getPersona().getId().equals(personaId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificacion", id));
        n.marcarLeida();
        return NotificacionResponse.de(n);
    }

    @Override
    public ConteoNotificacionesResponse marcarTodasLeidas() {
        notificaciones.findByPersonaIdAndLeidaFalseOrderByFechaHoraDesc(personaAutenticada.id())
                .forEach(Notificacion::marcarLeida);
        return new ConteoNotificacionesResponse(0);
    }
}
