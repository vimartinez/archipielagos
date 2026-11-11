package com.mijuego.archipielago.motor.resolucion;

import com.mijuego.archipielago.motor.mapa.IslaId;

/**
 * Cambio de dueño de una isla durante la ronda.
 *
 * @param duenoAnterior dueño previo, o {@code null} si la isla era neutral
 * @param nuevoDueno    jugador que pasa a controlar la isla
 */
public record Captura(IslaId isla, String duenoAnterior, String nuevoDueno) {
}
