package com.mijuego.archipielago.motor;

/**
 * Violación de una regla del juego: configuración inválida, orden inválida, jugador eliminado
 * o partida terminada. El mensaje, en español, identifica el valor o la orden infractora.
 */
public class ReglaInfringidaException extends RuntimeException {

    public ReglaInfringidaException(String mensaje) {
        super(mensaje);
    }
}
