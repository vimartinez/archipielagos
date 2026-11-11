package com.mijuego.archipielago.motor;

import com.mijuego.archipielago.motor.mapa.IslaId;
import com.mijuego.archipielago.motor.modelo.EstadoIsla;
import com.mijuego.archipielago.motor.modelo.EstadoPartida;
import com.mijuego.archipielago.motor.modelo.Flota;
import com.mijuego.archipielago.motor.modelo.Jugador;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Constructor de estados arbitrarios para probar la resolución de rondas. Por defecto todo es neutral. */
public final class EstadoDePrueba {

    private final List<String> apodos;
    private final Set<String> eliminados = new HashSet<>();
    private final Map<IslaId, EstadoIsla> islas = new EnumMap<>(IslaId.class);
    private final List<Flota> flotas = new ArrayList<>();
    private int ronda = 1;
    private int limite = 25;

    private EstadoDePrueba(List<String> apodos) {
        this.apodos = apodos;
        for (IslaId isla : IslaId.values()) {
            islas.put(isla, EstadoIsla.neutral(isla));
        }
    }

    public static EstadoDePrueba con(String... apodos) {
        return new EstadoDePrueba(List.of(apodos));
    }

    public EstadoDePrueba isla(IslaId isla, String dueno, int barcos) {
        islas.put(isla, new EstadoIsla(isla, dueno, barcos));
        return this;
    }

    /** Flota en tránsito; con {@code rondasRestantes = 1} llega en la próxima resolución. */
    public EstadoDePrueba flota(String dueno, int barcos, IslaId origen, IslaId destino, int rondasRestantes) {
        flotas.add(new Flota(dueno, barcos, origen, destino, rondasRestantes));
        return this;
    }

    /** Flota que llega a {@code destino} en la próxima resolución. */
    public EstadoDePrueba llega(String dueno, int barcos, IslaId origen, IslaId destino) {
        return flota(dueno, barcos, origen, destino, 1);
    }

    public EstadoDePrueba ronda(int ronda) {
        this.ronda = ronda;
        return this;
    }

    public EstadoDePrueba limite(int limite) {
        this.limite = limite;
        return this;
    }

    public EstadoDePrueba eliminado(String apodo) {
        eliminados.add(apodo);
        return this;
    }

    public EstadoPartida build() {
        List<Jugador> jugadores = apodos.stream().map(a -> new Jugador(a, !eliminados.contains(a))).toList();
        return new EstadoPartida(ronda, limite, true, jugadores, islas, flotas, List.of());
    }
}
