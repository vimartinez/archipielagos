package com.mijuego.archipielago.motor.resolucion;

import com.mijuego.archipielago.motor.ReglaInfringidaException;
import com.mijuego.archipielago.motor.mapa.IslaId;
import com.mijuego.archipielago.motor.mapa.Mapa;
import com.mijuego.archipielago.motor.modelo.EstadoIsla;
import com.mijuego.archipielago.motor.modelo.EstadoPartida;
import com.mijuego.archipielago.motor.modelo.Flota;
import com.mijuego.archipielago.motor.modelo.Jugador;
import com.mijuego.archipielago.motor.modelo.Orden;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolución determinista de una ronda como función pura: no modifica el estado recibido ni usa
 * aleatoriedad. Las islas se recorren por ordinal de {@link IslaId} y los jugadores por orden de unión.
 *
 * <p>Fases, en orden: (1) salida, (2) avance, (3) llegada, (4) combate, (5) captura, (6) producción,
 * (7) eliminaciones y fin de partida.
 */
public final class ResolutorRonda {

    private ResolutorRonda() {
    }

    /**
     * Resuelve la ronda actual.
     *
     * @param estado  estado al inicio de la ronda
     * @param ordenes órdenes de cada jugador; un jugador ausente no mueve barcos
     * @throws ReglaInfringidaException si la partida ha terminado o alguna orden no es válida
     */
    public static ResultadoRonda resolver(EstadoPartida estado, Map<String, List<Orden>> ordenes) {
        if (!estado.enCurso()) {
            throw new ReglaInfringidaException("La partida ha terminado");
        }
        ordenes.forEach((apodo, lista) -> ValidadorOrdenes.validar(estado, apodo, lista));
        return new Ronda(estado, ordenes).resolver();
    }

    /** Estado intermedio y mutable de una ronda en resolución. */
    private static final class Ronda {

        private final EstadoPartida inicial;
        private final Map<String, List<Orden>> ordenes;
        private final List<String> apodos;

        private final Map<IslaId, String> duenos = new EnumMap<>(IslaId.class);
        private final Map<IslaId, Integer> barcos = new EnumMap<>(IslaId.class);
        private List<Flota> flotas;
        private final Map<IslaId, List<Flota>> llegadas = new EnumMap<>(IslaId.class);
        /** Quién queda en cada isla tras el combate (puede ser el dueño previo o nadie). */
        private final Map<IslaId, String> poseedores = new EnumMap<>(IslaId.class);

        private final List<Combate> combates = new ArrayList<>();
        private final List<Captura> capturas = new ArrayList<>();

        Ronda(EstadoPartida inicial, Map<String, List<Orden>> ordenes) {
            this.inicial = inicial;
            this.ordenes = ordenes;
            this.apodos = inicial.jugadores().stream().map(Jugador::apodo).toList();
            for (EstadoIsla isla : inicial.islas().values()) {
                duenos.put(isla.isla(), isla.dueno());
                barcos.put(isla.isla(), isla.barcos());
            }
            this.flotas = new ArrayList<>(inicial.flotas());
        }

        ResultadoRonda resolver() {
            salida();
            avance();
            llegada();
            combate();
            captura();
            produccion();
            return finDeRonda();
        }

        /** (1) Cada orden resta sus barcos del origen y crea una flota con la distancia de la ruta. */
        private void salida() {
            for (Jugador jugador : inicial.jugadoresActivos()) {
                for (Orden orden : ordenes.getOrDefault(jugador.apodo(), List.of())) {
                    barcos.merge(orden.origen(), -orden.barcos(), Integer::sum);
                    int distancia = Mapa.distancia(orden.origen(), orden.destino()).orElseThrow();
                    flotas.add(new Flota(jugador.apodo(), orden.barcos(), orden.origen(), orden.destino(), distancia));
                }
            }
        }

        /** (2) Todas las flotas, incluidas las recién creadas, reducen en 1 sus rondas restantes. */
        private void avance() {
            flotas = new ArrayList<>(flotas.stream().map(Flota::avanzar).toList());
        }

        /** (3) Las flotas con 0 rondas restantes llegan a su destino; las demás siguen en tránsito. */
        private void llegada() {
            List<Flota> enTransito = new ArrayList<>();
            for (Flota flota : flotas) {
                if (flota.haLlegado()) {
                    llegadas.computeIfAbsent(flota.destino(), d -> new ArrayList<>()).add(flota);
                } else {
                    enTransito.add(flota);
                }
            }
            flotas = enTransito;
        }

        /**
         * (4) Algoritmo único de combate (design D5): suma de fuerzas por jugador, descarte de las nulas,
         * gana el máximo único con {@code max - segundo}; con el máximo empatado gana el defensor con 0 si
         * está entre los empatados y, si no, la isla conserva su dueño con 0.
         */
        private void combate() {
            for (Map.Entry<IslaId, List<Flota>> entrada : llegadas.entrySet()) {
                IslaId isla = entrada.getKey();
                String defensor = duenos.get(isla);
                Map<String, Integer> fuerzas = fuerzas(isla, defensor, entrada.getValue());

                if (fuerzas.size() == 1) {
                    Map.Entry<String, Integer> unica = fuerzas.entrySet().iterator().next();
                    poseedores.put(isla, unica.getKey());
                    barcos.put(isla, unica.getValue());
                    continue;
                }

                List<Map.Entry<String, Integer>> ordenadas = fuerzas.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                        .toList();
                int maxima = ordenadas.get(0).getValue();
                int segunda = ordenadas.get(1).getValue();
                String ganador;
                int restantes;
                if (maxima > segunda) {
                    ganador = ordenadas.get(0).getKey();
                    restantes = maxima - segunda;
                } else {
                    boolean defensorEmpatado = defensor != null && Objects.equals(fuerzas.get(defensor), maxima);
                    ganador = defensorEmpatado ? defensor : null;
                    restantes = 0;
                }
                poseedores.put(isla, ganador == null ? defensor : ganador);
                barcos.put(isla, restantes);
                combates.add(new Combate(isla, fuerzas, ganador, restantes));
            }
        }

        /** Fuerzas con al menos 1 barco: guarnición del dueño más flotas llegadas, en orden de unión. */
        private Map<String, Integer> fuerzas(IslaId isla, String defensor, List<Flota> llegadasIsla) {
            Map<String, Integer> fuerzas = new LinkedHashMap<>();
            for (String apodo : apodos) {
                int fuerza = apodo.equals(defensor) ? barcos.get(isla) : 0;
                for (Flota flota : llegadasIsla) {
                    if (flota.dueno().equals(apodo)) {
                        fuerza += flota.barcos();
                    }
                }
                if (fuerza > 0) {
                    fuerzas.put(apodo, fuerza);
                }
            }
            return fuerzas;
        }

        /** (5) Quien queda en una isla distinto de su dueño pasa a controlarla. */
        private void captura() {
            poseedores.forEach((isla, poseedor) -> {
                String anterior = duenos.get(isla);
                if (poseedor != null && !poseedor.equals(anterior)) {
                    duenos.put(isla, poseedor);
                    capturas.add(new Captura(isla, anterior, poseedor));
                }
            });
        }

        /** (6) Cada isla de un jugador activo gana 1 barco; las neutrales no producen. */
        private void produccion() {
            duenos.forEach((isla, dueno) -> {
                if (dueno != null && inicial.jugador(dueno).map(Jugador::activo).orElse(false)) {
                    barcos.merge(isla, 1, Integer::sum);
                }
            });
        }

        /** (7) Eliminaciones, fin de partida y avance de ronda. */
        private ResultadoRonda finDeRonda() {
            List<String> eliminados = new ArrayList<>();
            List<Jugador> jugadores = new ArrayList<>();
            for (Jugador jugador : inicial.jugadores()) {
                if (jugador.activo() && sinFuerzas(jugador.apodo())) {
                    eliminados.add(jugador.apodo());
                    jugadores.add(jugador.comoEliminado());
                } else {
                    jugadores.add(jugador);
                }
            }
            List<String> activos = jugadores.stream().filter(Jugador::activo).map(Jugador::apodo).toList();

            EstadoPartida provisional = construirEstado(inicial.ronda(), true, jugadores, List.of());
            List<String> ganadores = List.of();
            if (activos.size() == 1) {
                ganadores = activos;
            } else if (activos.isEmpty()) {
                ganadores = eliminados;
            } else if (inicial.ronda() >= inicial.limiteRondas()) {
                ganadores = ganadoresPorLimite(provisional, activos);
            }

            boolean terminada = !ganadores.isEmpty();
            int ronda = terminada ? inicial.ronda() : inicial.ronda() + 1;
            EstadoPartida estado = construirEstado(ronda, !terminada, jugadores, ganadores);
            InformeRonda informe = new InformeRonda(inicial.ronda(), combates, capturas, eliminados, ganadores);
            return new ResultadoRonda(estado, informe);
        }

        private boolean sinFuerzas(String apodo) {
            return duenos.values().stream().noneMatch(apodo::equals)
                    && flotas.stream().noneMatch(f -> f.dueno().equals(apodo));
        }

        /** Más islas; después más barcos totales; si persiste el empate, victoria compartida. */
        private static List<String> ganadoresPorLimite(EstadoPartida estado, List<String> activos) {
            int maxIslas = activos.stream().mapToInt(a -> estado.islasDe(a).size()).max().orElseThrow();
            List<String> conMasIslas = activos.stream().filter(a -> estado.islasDe(a).size() == maxIslas).toList();
            int maxBarcos = conMasIslas.stream().mapToInt(estado::barcosTotales).max().orElseThrow();
            return conMasIslas.stream().filter(a -> estado.barcosTotales(a) == maxBarcos).toList();
        }

        private EstadoPartida construirEstado(int ronda, boolean enCurso, List<Jugador> jugadores,
                                              List<String> ganadores) {
            Map<IslaId, EstadoIsla> islas = new EnumMap<>(IslaId.class);
            for (IslaId isla : IslaId.values()) {
                islas.put(isla, new EstadoIsla(isla, duenos.get(isla), barcos.get(isla)));
            }
            return new EstadoPartida(ronda, inicial.limiteRondas(), enCurso, jugadores, islas, flotas, ganadores);
        }
    }
}
