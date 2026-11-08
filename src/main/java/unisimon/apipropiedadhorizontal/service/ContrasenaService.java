package unisimon.apipropiedadhorizontal.service;

/** CU-002 Gestionar contrasena (cambio por el propio usuario autenticado). */
public interface ContrasenaService {

    void cambiar(String actual, String nueva);
}
