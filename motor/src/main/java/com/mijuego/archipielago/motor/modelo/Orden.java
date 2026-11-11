package com.mijuego.archipielago.motor.modelo;

import com.mijuego.archipielago.motor.mapa.IslaId;

/**
 * Orden de un jugador: enviar {@code barcos} de la isla {@code origen} a la isla {@code destino}.
 *
 * <p>No se valida al construirla: la validación contra el estado de la partida la hace el motor
 * al recibir el conjunto de órdenes.
 */
public record Orden(IslaId origen, IslaId destino, int barcos) {

    /** Descripción legible, p. ej. "3 de S1 a I1". */
    public String describir() {
        return "%d de %s a %s".formatted(barcos, origen, destino);
    }
}
