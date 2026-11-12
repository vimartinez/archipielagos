package com.mijuego.archipielago.servidor.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.mijuego.archipielago.motor.mapa.Mapa;
import com.mijuego.archipielago.motor.modelo.EstadoPartida;
import java.util.Collection;
import java.util.List;

/**
 * Estado público de una partida en línea, tal como lo ve un jugador.
 *
 * @param msRestantes milisegundos que quedan para que venza la ronda en curso (0 si la partida terminó)
 * @param confirmados jugadores que ya han confirmado sus órdenes en la ronda en curso
 */
@JsonTypeName("partida")
public record PartidaDto(
        String tuApodo,
        int ronda,
        int limiteRondas,
        boolean enCurso,
        long msRestantes,
        List<JugadorDto> jugadores,
        List<IslaDto> islas,
        List<FlotaDto> flotas,
        List<String> confirmados,
        List<String> ganadores) implements MensajeSaliente {

    /** Un jugador con sus islas y sus barcos totales (islas más flotas). */
    public record JugadorDto(String apodo, boolean activo, int islas, int barcos) {
    }

    /** Una isla; {@code dueno} es null si es neutral. */
    public record IslaDto(String id, String dueno, int barcos) {
    }

    /** Una flota en tránsito, con la distancia total de su ruta para dibujar su avance. */
    public record FlotaDto(String dueno, int barcos, String origen, String destino, int rondasRestantes, int distancia) {
    }

    public static PartidaDto de(EstadoPartida estado, String tuApodo, long msRestantes, Collection<String> confirmados) {
        return new PartidaDto(
                tuApodo,
                estado.ronda(),
                estado.limiteRondas(),
                estado.enCurso(),
                msRestantes,
                estado.jugadores().stream()
                        .map(j -> new JugadorDto(j.apodo(), j.activo(), estado.islasDe(j.apodo()).size(),
                                estado.barcosTotales(j.apodo())))
                        .toList(),
                estado.islas().values().stream()
                        .map(i -> new IslaDto(i.isla().name(), i.dueno(), i.barcos()))
                        .toList(),
                estado.flotas().stream()
                        .map(f -> new FlotaDto(f.dueno(), f.barcos(), f.origen().name(), f.destino().name(),
                                f.rondasRestantes(), Mapa.distancia(f.origen(), f.destino()).orElse(f.rondasRestantes())))
                        .toList(),
                List.copyOf(confirmados),
                estado.ganadores());
    }
}
