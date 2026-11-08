package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.domain.enums.EResultadoContacto;
import unisimon.apipropiedadhorizontal.domain.model.RegistroVisita;

import java.time.LocalDateTime;

/**
 * {@code estado}: {@code Dentro} (ingreso autorizado y sin salida), {@code Salio} o {@code Negado}.
 * {@code parqueadero} es el nombre (torre-numero) del parqueadero asignado, o null.
 */
public record RegistroVisitaResponse(Long id, Long visitanteId, String visitante, String numeroDocumento,
                                     Long inmuebleId, String inmueble, boolean conPreRegistro, Long preRegistroId,
                                     Long residenteContactadoId, String residenteContactado,
                                     EResultadoContacto respuestaResidente, boolean ingresoAutorizado,
                                     String motivoNegacion, String tipoVehiculo, Long parqueaderoId,
                                     String parqueadero, LocalDateTime fechaHoraLlegada,
                                     LocalDateTime fechaHoraEntrada, LocalDateTime fechaHoraSalida, boolean dentro,
                                     String estado) {

    public static RegistroVisitaResponse de(RegistroVisita r) {
        var residente = r.getResidenteContactado();
        var parqueadero = r.getParqueaderoAsignado();
        String estado = !r.isIngresoAutorizado() ? "Negado" : r.getFechaHoraSalida() != null ? "Salio" : "Dentro";
        return new RegistroVisitaResponse(r.getId(), r.getVisitante().getId(), r.getVisitante().getNombreCompleto(),
                r.getVisitante().getNumeroDocumento(), r.getInmueble().getId(),
                r.getInmueble().getTorreBloque() + "-" + r.getInmueble().getNumero(), r.getPreRegistro() != null,
                r.getPreRegistro() == null ? null : r.getPreRegistro().getId(),
                residente == null ? null : residente.getId(),
                residente == null ? null : residente.getNombres() + " " + residente.getApellidos(),
                r.getRespuestaResidente(), r.isIngresoAutorizado(), r.getMotivoNegacion(),
                r.getTipoVehiculo() == null ? null : r.getTipoVehiculo().getNombre(),
                parqueadero == null ? null : parqueadero.getId(),
                parqueadero == null ? null : parqueadero.getTorreBloque() + "-" + parqueadero.getNumero(),
                r.getFechaHoraLlegada(), r.getFechaHoraEntrada(), r.getFechaHoraSalida(), r.estaDentro(), estado);
    }
}
