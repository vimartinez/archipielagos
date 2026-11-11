package com.mijuego.archipielago.motor.modelo;

import com.mijuego.archipielago.motor.mapa.IslaId;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Instantánea inmutable y pública del estado de una partida.
 *
 * @param ronda         ronda actual (la que se está jugando o, si terminó, la última resuelta)
 * @param limiteRondas  límite de rondas configurado
 * @param enCurso       si la partida sigue en curso
 * @param jugadores     jugadores en orden de unión, activos y eliminados
 * @param islas         estado de las 13 islas, en orden de {@link IslaId}
 * @param flotas        flotas en tránsito
 * @param ganadores     ganadores en orden de unión (vacío mientras la partida esté en curso)
 */
public record EstadoPartida(
        int ronda,
        int limiteRondas,
        boolean enCurso,
        List<Jugador> jugadores,
        Map<IslaId, EstadoIsla> islas,
        List<Flota> flotas,
        List<String> ganadores) {

    public EstadoPartida {
        jugadores = List.copyOf(jugadores);
        flotas = List.copyOf(flotas);
        ganadores = List.copyOf(ganadores);
        EnumMap<IslaId, EstadoIsla> copia = new EnumMap<>(IslaId.class);
        for (IslaId isla : IslaId.values()) {
            EstadoIsla estado = Objects.requireNonNull(islas.get(isla), () -> "Falta la isla " + isla);
            if (estado.isla() != isla) {
                throw new IllegalArgumentException("La isla " + isla + " tiene un estado de " + estado.isla());
            }
            copia.put(isla, estado);
        }
        islas = Collections.unmodifiableMap(copia);
    }

    public EstadoIsla isla(IslaId isla) {
        return islas.get(isla);
    }

    public Optional<Jugador> jugador(String apodo) {
        return jugadores.stream().filter(j -> j.apodo().equals(apodo)).findFirst();
    }

    public List<Jugador> jugadoresActivos() {
        return jugadores.stream().filter(Jugador::activo).toList();
    }

    public List<Jugador> jugadoresEliminados() {
        return jugadores.stream().filter(Jugador::eliminado).toList();
    }

    public List<EstadoIsla> islasDe(String apodo) {
        return islas.values().stream().filter(i -> i.esDe(apodo)).toList();
    }

    public List<Flota> flotasDe(String apodo) {
        return flotas.stream().filter(f -> f.dueno().equals(apodo)).toList();
    }

    /** Barcos totales de un jugador: los de sus islas más los de sus flotas en tránsito. */
    public int barcosTotales(String apodo) {
        return islasDe(apodo).stream().mapToInt(EstadoIsla::barcos).sum()
                + flotasDe(apodo).stream().mapToInt(Flota::barcos).sum();
    }
}
