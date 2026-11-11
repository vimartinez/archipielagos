package com.mijuego.archipielago.motor.modelo;

import com.mijuego.archipielago.motor.ReglaInfringidaException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuración validada de una partida: apodos de los jugadores en orden de unión y límite de rondas.
 *
 * @throws ReglaInfringidaException si la configuración no es válida
 */
public record ConfiguracionPartida(List<String> apodos, int limiteRondas) {

    public static final int MIN_JUGADORES = 2;
    public static final int MAX_JUGADORES = 4;
    public static final int MIN_LIMITE_RONDAS = 10;
    public static final int MAX_LIMITE_RONDAS = 50;
    public static final int LIMITE_RONDAS_POR_DEFECTO = 25;

    public ConfiguracionPartida {
        if (apodos == null || apodos.size() < MIN_JUGADORES || apodos.size() > MAX_JUGADORES) {
            int cantidad = apodos == null ? 0 : apodos.size();
            throw new ReglaInfringidaException("Se necesitan entre %d y %d jugadores (hay %d)"
                    .formatted(MIN_JUGADORES, MAX_JUGADORES, cantidad));
        }
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < apodos.size(); i++) {
            String apodo = apodos.get(i);
            if (apodo == null || apodo.isBlank()) {
                throw new ReglaInfringidaException("El apodo del jugador %d no es válido: está vacío"
                        .formatted(i + 1));
            }
            if (!vistos.add(apodo)) {
                throw new ReglaInfringidaException("El apodo '%s' no es válido: está repetido".formatted(apodo));
            }
        }
        if (limiteRondas < MIN_LIMITE_RONDAS || limiteRondas > MAX_LIMITE_RONDAS) {
            throw new ReglaInfringidaException("El límite de rondas debe estar entre %d y %d (es %d)"
                    .formatted(MIN_LIMITE_RONDAS, MAX_LIMITE_RONDAS, limiteRondas));
        }
        apodos = List.copyOf(apodos);
    }

    /** Crea la configuración usando el límite por defecto (25) si {@code limiteRondas} es null. */
    public static ConfiguracionPartida de(List<String> apodos, Integer limiteRondas) {
        return new ConfiguracionPartida(apodos, limiteRondas == null ? LIMITE_RONDAS_POR_DEFECTO : limiteRondas);
    }
}
