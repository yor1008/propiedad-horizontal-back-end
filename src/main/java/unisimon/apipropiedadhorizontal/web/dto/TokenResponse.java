package unisimon.apipropiedadhorizontal.web.dto;

import unisimon.apipropiedadhorizontal.security.TokenEmitido;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static TokenResponse de(TokenEmitido token) {
        return new TokenResponse(token.accessToken(), token.tipo(), token.expiraEnSegundos());
    }
}
