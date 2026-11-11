package com.mijuego.archipielago.motor.modelo;

import com.mijuego.archipielago.motor.mapa.IslaId;
import java.util.Objects;

/** Flota en tránsito, visible para todos los jugadores. */
public record Flota(String dueno, int barcos, IslaId origen, IslaId destino, int rondasRestantes) {

    public Flota {
        Objects.requireNonNull(dueno, "dueno");
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(destino, "destino");
        if (barcos < 1) {
            throw new IllegalArgumentException("Una flota debe tener al menos 1 barco: " + barcos);
        }
        if (rondasRestantes < 0) {
            throw new IllegalArgumentException("Las rondas restantes no pueden ser negativas: " + rondasRestantes);
        }
    }

    /** La misma flota con una ronda menos de trayecto. */
    public Flota avanzar() {
        return new Flota(dueno, barcos, origen, destino, rondasRestantes - 1);
    }

    public boolean haLlegado() {
        return rondasRestantes == 0;
    }
}
