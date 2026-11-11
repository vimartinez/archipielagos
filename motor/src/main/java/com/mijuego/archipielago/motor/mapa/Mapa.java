package com.mijuego.archipielago.motor.mapa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Mapa fijo y simétrico de Archipiélago: 13 islas y 24 rutas bidireccionales.
 *
 * <p>Para cada índice i en 1..4, con j = i+1 y k = i-1 (en anillo):
 * <ul>
 *   <li>Si–Ii: distancia 1</li>
 *   <li>Si–Bij y Si–Bki: distancia 3</li>
 *   <li>Ii–Bij y Ii–Bki: distancia 1</li>
 *   <li>Ii–C: distancia 2</li>
 * </ul>
 */
public final class Mapa {

    private static final List<IslaId> ISLAS = List.of(IslaId.values());
    private static final List<Ruta> RUTAS = construirRutas();
    private static final Map<IslaId, Map<IslaId, Ruta>> ADYACENCIA = construirAdyacencia(RUTAS);

    private static final Map<Integer, List<IslaId>> SALIDAS = Map.of(
            2, List.of(IslaId.S1, IslaId.S3),
            3, List.of(IslaId.S1, IslaId.S2, IslaId.S3),
            4, List.of(IslaId.S1, IslaId.S2, IslaId.S3, IslaId.S4));

    private Mapa() {
    }

    /** Las 13 islas del mapa, en orden determinista. */
    public static List<IslaId> islas() {
        return ISLAS;
    }

    /** Las 24 rutas del mapa. */
    public static List<Ruta> rutas() {
        return RUTAS;
    }

    /** La ruta directa entre dos islas, si existe. */
    public static Optional<Ruta> ruta(IslaId origen, IslaId destino) {
        if (origen == null || destino == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ADYACENCIA.get(origen).get(destino));
    }

    /** Indica si hay una ruta directa entre dos islas. */
    public static boolean hayRuta(IslaId origen, IslaId destino) {
        return ruta(origen, destino).isPresent();
    }

    /** Distancia en rondas entre dos islas conectadas directamente, o vacío si no lo están. */
    public static OptionalInt distancia(IslaId origen, IslaId destino) {
        return ruta(origen, destino).map(r -> OptionalInt.of(r.distancia())).orElse(OptionalInt.empty());
    }

    /**
     * Islas de salida para un número de jugadores, en el orden en que se unieron.
     *
     * @throws IllegalArgumentException si el número de jugadores no está entre 2 y 4
     */
    public static List<IslaId> salidas(int numeroJugadores) {
        List<IslaId> salidas = SALIDAS.get(numeroJugadores);
        if (salidas == null) {
            throw new IllegalArgumentException("Número de jugadores no soportado: " + numeroJugadores);
        }
        return salidas;
    }

    private static List<Ruta> construirRutas() {
        List<Ruta> rutas = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            int k = IslaId.anterior(i);
            IslaId s = IslaId.salida(i);
            IslaId interior = IslaId.interior(i);
            IslaId bahiaSiguiente = IslaId.bahia(i);
            IslaId bahiaAnterior = IslaId.bahia(k);
            rutas.add(new Ruta(s, interior, 1));
            rutas.add(new Ruta(s, bahiaSiguiente, 3));
            rutas.add(new Ruta(s, bahiaAnterior, 3));
            rutas.add(new Ruta(interior, bahiaSiguiente, 1));
            rutas.add(new Ruta(interior, bahiaAnterior, 1));
            rutas.add(new Ruta(interior, IslaId.C, 2));
        }
        return List.copyOf(rutas);
    }

    private static Map<IslaId, Map<IslaId, Ruta>> construirAdyacencia(List<Ruta> rutas) {
        Map<IslaId, Map<IslaId, Ruta>> adyacencia = new EnumMap<>(IslaId.class);
        Arrays.stream(IslaId.values()).forEach(isla -> adyacencia.put(isla, new EnumMap<>(IslaId.class)));
        for (Ruta ruta : rutas) {
            if (adyacencia.get(ruta.a()).put(ruta.b(), ruta) != null) {
                throw new IllegalStateException("Ruta duplicada: " + ruta);
            }
            adyacencia.get(ruta.b()).put(ruta.a(), ruta);
        }
        adyacencia.replaceAll((isla, vecinas) -> Collections.unmodifiableMap(vecinas));
        return Collections.unmodifiableMap(adyacencia);
    }
}
