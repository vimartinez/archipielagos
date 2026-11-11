package com.mijuego.archipielago.motor.mapa;

import java.util.Objects;

/**
 * Ruta bidireccional entre dos islas con su distancia en rondas.
 *
 * <p>Los extremos se normalizan por ordinal, de modo que {@code new Ruta(A, B, d)} y
 * {@code new Ruta(B, A, d)} son iguales.
 */
public record Ruta(IslaId a, IslaId b, int distancia) {

    public Ruta {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        if (a == b) {
            throw new IllegalArgumentException("Una ruta no puede unir una isla consigo misma: " + a);
        }
        if (distancia < 1) {
            throw new IllegalArgumentException("La distancia debe ser al menos 1: " + distancia);
        }
        if (a.ordinal() > b.ordinal()) {
            IslaId tmp = a;
            a = b;
            b = tmp;
        }
    }

    /** Indica si la ruta une las dos islas dadas, en cualquier sentido. */
    public boolean conecta(IslaId x, IslaId y) {
        return (a == x && b == y) || (a == y && b == x);
    }
}
