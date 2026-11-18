package unisimon.apipropiedadhorizontal.domain.model;

import org.junit.jupiter.api.Test;
import unisimon.apipropiedadhorizontal.domain.enums.ECategoriaInmueble;
import unisimon.apipropiedadhorizontal.domain.enums.EProcesoEstado;
import unisimon.apipropiedadhorizontal.domain.enums.ERolCuenta;
import unisimon.apipropiedadhorizontal.domain.enums.ETipoCatalogo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Operaciones del diagrama de clases que viven en las entidades (sin base de datos ni Spring). */
class OperacionesDelDiagramaTest {

    private static final LocalDate AYER = LocalDate.now().minusDays(1);

    // ---------- CuentaUsuario: nota del diagrama ----------

    @Test
    void cuentaPersonalNoTienePersonaYSoloEsDeAdministradorOVigilante() {
        CuentaUsuario vigilante = CuentaUsuario.crearCuentaPersonal("v@ph.com", "hash", ERolCuenta.VIGILANTE);
        assertThat(vigilante.getPersona()).isNull();
        assertThat(vigilante.getRol()).isEqualTo(ERolCuenta.VIGILANTE);
        assertThat(vigilante.isActiva()).isTrue();
        assertThat(CuentaUsuario.crearCuentaPersonal("a@ph.com", "hash", ERolCuenta.ADMINISTRADOR).getPersona()).isNull();

        assertThatThrownBy(() -> CuentaUsuario.crearCuentaPersonal("p@ph.com", "hash", ERolCuenta.PERSONA_VINCULADA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CuentaUsuario.crearCuentaPersonal("p@ph.com", "hash", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cuentaDePersonaVinculadaExigePersonaConTipo() {
        Persona persona = Persona.builder().id(1L).tipoPersona(residente()).build();
        CuentaUsuario cuenta = CuentaUsuario.crearCuentaPersonaVinculada("ana@ph.com", "hash", persona);
        assertThat(cuenta.getRol()).isEqualTo(ERolCuenta.PERSONA_VINCULADA);
        assertThat(cuenta.getPersona()).isSameAs(persona);

        assertThatThrownBy(() -> CuentaUsuario.crearCuentaPersonaVinculada("x@ph.com", "hash", null))
                .isInstanceOf(IllegalArgumentException.class);
        Persona sinTipo = Persona.builder().id(2L).build();
        assertThatThrownBy(() -> CuentaUsuario.crearCuentaPersonaVinculada("x@ph.com", "hash", sinTipo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- Inmueble ----------

    @Test
    void inmuebleDevuelveResidentesYPropietarioVigentes() {
        Inmueble ref = Inmueble.builder().id(10L).build();
        TipoPersona propietarioResidente = TipoPersona.builder().nombre("PR").esPropietario(true).esResidente(true).build();
        Persona ana = Persona.builder().id(1L).tipoPersona(residente()).inmueble(ref).fechaInicioVinculo(AYER).build();
        Persona luis = Persona.builder().id(2L).tipoPersona(propietarioResidente).inmueble(ref).fechaInicioVinculo(AYER).build();
        Persona exResidente = Persona.builder().id(3L).tipoPersona(residente()).inmueble(ref)
                .fechaInicioVinculo(AYER.minusDays(10)).fechaFinVinculo(AYER.minusDays(2)).build();
        Inmueble inmueble = Inmueble.builder().id(10L).personas(List.of(ana, luis, exResidente)).build();

        assertThat(inmueble.obtenerResidentesVigentes()).containsExactly(ana, luis);
        assertThat(inmueble.obtenerPropietarioVigente()).isSameAs(luis);
        assertThat(Inmueble.builder().id(11L).build().obtenerPropietarioVigente()).isNull();
    }

    @Test
    void inmuebleRegistraYLlevaSusParqueaderos() {
        ValorCatalogo apto = ValorCatalogo.builder().build();
        apto.registrar(ETipoCatalogo.TIPO_INMUEBLE, "Apartamento");
        Inmueble inmueble = Inmueble.builder().id(10L).build();
        inmueble.registrar("A", "101", 1, apto);
        assertThat(inmueble.getTorreBloque()).isEqualTo("A");
        assertThat(inmueble.getCategoria()).isEqualTo(ECategoriaInmueble.UNIDAD_PRIVADA);
        assertThat(inmueble.isActivo()).isTrue();

        Inmueble parqueadero = Inmueble.builder().id(20L).categoria(ECategoriaInmueble.PARQUEADERO).build();
        inmueble.asignarParqueadero(parqueadero);
        assertThat(inmueble.obtenerParqueaderos()).containsExactly(parqueadero);
        assertThat(parqueadero.getInmuebleAsociado()).isSameAs(inmueble);

        inmueble.retirarParqueadero(parqueadero);
        assertThat(inmueble.obtenerParqueaderos()).isEmpty();
        assertThat(parqueadero.getInmuebleAsociado()).isNull();
    }

    // ---------- Estado / TransicionEstado ----------

    @Test
    void estadoSabeAQueEstadosPuedeCambiar() {
        Estado radicada = estado(1L, "Radicada");
        Estado tramite = estado(2L, "En tramite");
        Estado respondida = estado(3L, "Respondida");
        TransicionEstado aTramite = TransicionEstado.builder().estadoOrigen(radicada).estadoDestino(tramite).build();
        Estado origen = Estado.builder().id(1L).proceso(EProcesoEstado.PQRS).nombre("Radicada")
                .transiciones(List.of(aTramite)).build();

        assertThat(aTramite.esPermitida(radicada, tramite)).isTrue();
        assertThat(aTramite.esPermitida(tramite, radicada)).isFalse();
        assertThat(origen.puedeCambiarA(tramite)).isTrue();
        assertThat(origen.puedeCambiarA(respondida)).isFalse();
    }

    // ---------- registrar(...) y demas operaciones ----------

    @Test
    void registrarFijaLosDatosDelDiagrama() {
        Vehiculo vehiculo = Vehiculo.builder().build();
        vehiculo.registrar(" abc123 ", null, "Mazda", "Rojo");
        assertThat(vehiculo.getPlaca()).isEqualTo("ABC123");
        assertThat(vehiculo.isActivo()).isTrue();

        Persona persona = Persona.builder().tipoPersona(residente()).build();
        persona.registrar(null, "1001", "Ana", "Ruiz", "300", "ana@ph.com", true);
        assertThat(persona.getFechaAutorizacionDatos()).isEqualTo(LocalDate.now());
        assertThat(persona.isActiva()).isTrue();
        Persona sinAutorizar = Persona.builder().build();
        sinAutorizar.registrar(null, "1002", "Luis", "Paz", null, null, false);
        assertThat(sinAutorizar.getFechaAutorizacionDatos()).isNull();

        Visitante visitante = Visitante.builder().build();
        visitante.registrar("Carlos Gomez", "555");
        assertThat(visitante.getNumeroDocumento()).isEqualTo("555");

        TipoPersona tipo = TipoPersona.builder().build();
        tipo.registrar("Propietario", "Dueno", true, false);
        assertThat(tipo.isEsPropietario()).isTrue();
        assertThat(tipo.isActivo()).isTrue();
    }

    @Test
    void pqrsRespondidaNoVuelveATramiteYMantenimientoCanceladoNoInicia() {
        Pqrs pqrs = Pqrs.builder().build();
        pqrs.radicar(null, "Ruido", "Fiesta");
        pqrs.ponerEnGestion();
        pqrs.responder("Listo");
        assertThatThrownBy(pqrs::ponerEnGestion).isInstanceOf(IllegalStateException.class);

        Mantenimiento mantenimiento = Mantenimiento.builder().build();
        mantenimiento.registrar(null, "Piscina", "Limpieza", LocalDateTime.now().plusDays(1), null, "Juan");
        mantenimiento.iniciar();
        mantenimiento.cancelar("Lluvia");
        assertThatThrownBy(mantenimiento::iniciar).isInstanceOf(IllegalStateException.class);
    }

    private static TipoPersona residente() {
        return TipoPersona.builder().nombre("Residente").esResidente(true).build();
    }

    private static Estado estado(Long id, String nombre) {
        return Estado.builder().id(id).proceso(EProcesoEstado.PQRS).nombre(nombre).build();
    }
}
