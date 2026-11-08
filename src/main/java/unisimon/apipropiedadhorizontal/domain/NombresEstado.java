package unisimon.apipropiedadhorizontal.domain;

/** Nombres de los estados sembrados por {@code EstadosIniciales}; los servicios los usan para pedir transiciones. */
public final class NombresEstado {

    public static final String CORRESPONDENCIA_PENDIENTE = "Pendiente";
    public static final String CORRESPONDENCIA_ENTREGADA = "Entregada";

    public static final String RESERVA_PENDIENTE = "Pendiente";
    public static final String RESERVA_APROBADA = "Aprobada";
    public static final String RESERVA_RECHAZADA = "Rechazada";
    public static final String RESERVA_CANCELADA = "Cancelada";
    public static final String RESERVA_FINALIZADA = "Finalizada";

    public static final String MANTENIMIENTO_PROGRAMADO = "Programado";
    public static final String MANTENIMIENTO_EN_CURSO = "En curso";
    public static final String MANTENIMIENTO_COMPLETADO = "Completado";
    public static final String MANTENIMIENTO_CANCELADO = "Cancelado";

    public static final String PQRS_RADICADA = "Radicada";
    public static final String PQRS_EN_TRAMITE = "En tramite";
    public static final String PQRS_RESPONDIDA = "Respondida";

    private NombresEstado() {
    }
}
