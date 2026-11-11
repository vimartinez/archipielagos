package com.mijuego.archipielago.motor.modelo;

import java.util.Objects;

/** Un jugador de la partida y si sigue activo o ha sido eliminado. */
public record Jugador(String apodo, boolean activo) {

    public Jugador {
        Objects.requireNonNull(apodo, "apodo");
    }

    public boolean eliminado() {
        return !activo;
    }

    /** El mismo jugador marcado como eliminado. */
    public Jugador comoEliminado() {
        return new Jugador(apodo, false);
    }
}
