package com.mijuego.archipielago.motor.mapa;

/**
 * Identificadores de las 13 islas del mapa fijo.
 *
 * <p>El orden de declaración (ordinal) es el orden determinista en que el motor recorre las islas.
 */
public enum IslaId {
    S1, S2, S3, S4,
    B12, B23, B34, B41,
    I1, I2, I3, I4,
    C;

    /** Isla de salida de índice {@code n} (1..4). */
    public static IslaId salida(int n) {
        return valueOf("S" + validarIndice(n));
    }

    /** Isla interior de índice {@code n} (1..4). */
    public static IslaId interior(int n) {
        return valueOf("I" + validarIndice(n));
    }

    /** Bahía situada entre los índices {@code n} y {@code n+1} (con 4 seguido de 1). */
    public static IslaId bahia(int n) {
        validarIndice(n);
        return valueOf("B" + n + siguiente(n));
    }

    /** Índice siguiente en el anillo 1..4 (4 pasa a 1). */
    public static int siguiente(int n) {
        return n % 4 + 1;
    }

    /** Índice anterior en el anillo 1..4 (1 pasa a 4). */
    public static int anterior(int n) {
        return (n + 2) % 4 + 1;
    }

    private static int validarIndice(int n) {
        if (n < 1 || n > 4) {
            throw new IllegalArgumentException("Índice de isla fuera de rango: " + n);
        }
        return n;
    }
}
