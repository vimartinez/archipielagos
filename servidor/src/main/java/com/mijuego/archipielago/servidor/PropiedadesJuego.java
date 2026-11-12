package com.mijuego.archipielago.servidor;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parámetros configurables del juego en línea (prefijo {@code archipielago}).
 *
 * @param duracionRonda  tiempo límite de cada ronda
 * @param intervaloPing  cada cuánto se envía un ping a cada conexión
 * @param limitePong     tiempo sin pong tras el que una conexión se da por muerta y se cierra
 */
@ConfigurationProperties("archipielago")
public record PropiedadesJuego(
        @DefaultValue("60s") Duration duracionRonda,
        @DefaultValue("25s") Duration intervaloPing,
        @DefaultValue("60s") Duration limitePong) {
}
