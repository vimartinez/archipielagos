package com.mijuego.archipielago.servidor.sala;

/** Petición de un jugador que no se puede atender por las reglas de las salas. El mensaje es legible, en español. */
public class PeticionRechazadaException extends RuntimeException {

    public PeticionRechazadaException(String mensaje) {
        super(mensaje);
    }
}
