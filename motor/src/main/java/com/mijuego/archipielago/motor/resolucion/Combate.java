package com.mijuego.archipielago.motor.resolucion;

import com.mijuego.archipielago.motor.mapa.IslaId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Combate ocurrido en una isla.
 *
 * @param isla            isla del combate
 * @param fuerzas         fuerza de cada jugador implicado, en orden de unión
 * @param ganador         ganador del combate, o {@code null} si no hubo ganador único
 * @param barcosRestantes barcos que quedan en la isla tras el combate
 */
public record Combate(IslaId isla, Map<String, Integer> fuerzas, String ganador, int barcosRestantes) {

    public Combate {
        fuerzas = Collections.unmodifiableMap(new LinkedHashMap<>(fuerzas));
    }

    public boolean hayGanador() {
        return ganador != null;
    }

    public Optional<String> ganadorOpcional() {
        return Optional.ofNullable(ganador);
    }
}
