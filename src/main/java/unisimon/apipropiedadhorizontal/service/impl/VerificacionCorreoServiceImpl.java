package unisimon.apipropiedadhorizontal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unisimon.apipropiedadhorizontal.domain.model.VerificacionCorreo;
import unisimon.apipropiedadhorizontal.exception.ReglaNegocioException;
import unisimon.apipropiedadhorizontal.repository.VerificacionCorreoRepository;
import unisimon.apipropiedadhorizontal.service.EnviadorCorreo;
import unisimon.apipropiedadhorizontal.service.VerificacionCorreoService;
import unisimon.apipropiedadhorizontal.util.Correos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class VerificacionCorreoServiceImpl implements VerificacionCorreoService {

    static final int MAX_INTENTOS = 5;
    private static final Duration VIGENCIA = Duration.ofMinutes(10);
    private static final String MENSAJE_INVALIDO = "No se pudo verificar el correo: el codigo es invalido o vencido. "
            + "Si el codigo nunca llego, revise que el correo exista y solicite uno nuevo";

    private final VerificacionCorreoRepository verificaciones;
    private final EnviadorCorreo enviador;
    private final Clock reloj;
    private final SecureRandom azar = new SecureRandom();

    public VerificacionCorreoServiceImpl(VerificacionCorreoRepository verificaciones, EnviadorCorreo enviador, Clock reloj) {
        this.verificaciones = verificaciones;
        this.enviador = enviador;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void enviarCodigo(String correoIngresado) {
        String correo = Correos.normalizar(correoIngresado);
        String codigo = "%06d".formatted(azar.nextInt(1_000_000));
        LocalDateTime expira = LocalDateTime.now(reloj).plus(VIGENCIA);
        verificaciones.findByCorreo(correo).ifPresentOrElse(
                v -> v.renovar(huella(correo, codigo), expira),
                () -> verificaciones.save(VerificacionCorreo.builder()
                        .correo(correo).hashCodigo(huella(correo, codigo)).expiraEn(expira).build()));
        enviador.enviar(correo, "Código de verificación",
                "Su código de verificación es: " + codigo + "\n\n"
                        + "Es válido por " + VIGENCIA.toMinutes() + " minutos. Entréguelo a la administración del conjunto "
                        + "para terminar su registro.\nSi usted no lo esperaba, ignore este mensaje.");
    }

    @Override
    @Transactional(noRollbackFor = ReglaNegocioException.class)
    public void confirmar(String correoIngresado, String codigo) {
        String correo = Correos.normalizar(correoIngresado);
        VerificacionCorreo v = verificaciones.findByCorreo(correo)
                .orElseThrow(() -> new ReglaNegocioException(MENSAJE_INVALIDO));
        if (v.estaVencido(LocalDateTime.now(reloj)) || v.getIntentos() >= MAX_INTENTOS) {
            verificaciones.delete(v);
            throw new ReglaNegocioException(MENSAJE_INVALIDO);
        }
        boolean coincide = codigo != null && MessageDigest.isEqual(
                huella(correo, codigo.trim()).getBytes(StandardCharsets.UTF_8),
                v.getHashCodigo().getBytes(StandardCharsets.UTF_8));
        if (!coincide) {
            v.registrarIntentoFallido();
            throw new ReglaNegocioException(MENSAJE_INVALIDO);
        }
        verificaciones.delete(v);
    }

    /** SHA-256 de correo + codigo: el codigo (1 millon de combinaciones) vence en minutos y tiene intentos limitados. */
    private static String huella(String correo, String codigo) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest((correo + ":" + codigo).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
