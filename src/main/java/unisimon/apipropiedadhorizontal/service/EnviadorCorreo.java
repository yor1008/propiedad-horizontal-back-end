package unisimon.apipropiedadhorizontal.service;

/**
 * Puerto de salida para enviar correos (hoy por SMTP). Los casos de uso dependen de esta interfaz, de modo que cambiar
 * a la API de un proveedor no los toca. El envio es "mejor esfuerzo": no lanza excepciones ni bloquea al llamador.
 */
public interface EnviadorCorreo {

    void enviar(String destinatario, String asunto, String texto);
}
