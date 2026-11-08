package unisimon.apipropiedadhorizontal.security;

/** Token emitido: el cliente lo envia como {@code Authorization: Bearer <accessToken>}. */
public record TokenEmitido(String accessToken, String tipo, long expiraEnSegundos) {

    public static final String TIPO_BEARER = "Bearer";
}
