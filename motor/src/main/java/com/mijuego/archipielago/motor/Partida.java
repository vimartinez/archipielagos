package com.mijuego.archipielago.motor;

import com.mijuego.archipielago.motor.mapa.IslaId;
import com.mijuego.archipielago.motor.mapa.Mapa;
import com.mijuego.archipielago.motor.modelo.ConfiguracionPartida;
import com.mijuego.archipielago.motor.modelo.EstadoIsla;
import com.mijuego.archipielago.motor.modelo.EstadoPartida;
import com.mijuego.archipielago.motor.modelo.Jugador;
import com.mijuego.archipielago.motor.modelo.Orden;
import com.mijuego.archipielago.motor.resolucion.InformeRonda;
import com.mijuego.archipielago.motor.resolucion.ResolutorRonda;
import com.mijuego.archipielago.motor.resolucion.ResultadoRonda;
import com.mijuego.archipielago.motor.resolucion.ValidadorOrdenes;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fachada pública del motor: guarda el estado actual de una partida y las órdenes pendientes de la ronda.
 *
 * <p><strong>No es thread-safe.</strong> Quien la use desde varios hilos debe serializar el acceso
 * a cada partida.
 */
public final class Partida {

    /** Barcos con los que empieza cada jugador en su isla de salida. */
    public static final int BARCOS_INICIALES = 5;

    private EstadoPartida estado;
    private final Map<String, List<Orden>> ordenesPendientes = new LinkedHashMap<>();

    private Partida(EstadoPartida estado) {
        this.estado = estado;
    }

    /**
     * Crea una partida.
     *
     * @param apodos       apodos de 2 a 4 jugadores, no vacíos y distintos, en orden de unión
     * @param limiteRondas límite de rondas entre 10 y 50, o {@code null} para usar 25
     * @throws ReglaInfringidaException si la configuración no es válida
     */
    public static Partida crear(List<String> apodos, Integer limiteRondas) {
        ConfiguracionPartida config = ConfiguracionPartida.de(apodos, limiteRondas);
        List<IslaId> salidas = Mapa.salidas(config.apodos().size());

        Map<IslaId, EstadoIsla> islas = new EnumMap<>(IslaId.class);
        Mapa.islas().forEach(isla -> islas.put(isla, EstadoIsla.neutral(isla)));
        for (int i = 0; i < salidas.size(); i++) {
            IslaId salida = salidas.get(i);
            islas.put(salida, new EstadoIsla(salida, config.apodos().get(i), BARCOS_INICIALES));
        }
        List<Jugador> jugadores = config.apodos().stream().map(a -> new Jugador(a, true)).toList();
        return new Partida(new EstadoPartida(1, config.limiteRondas(), true, jugadores, islas, List.of(), List.of()));
    }

    /** Crea una partida con el límite de rondas por defecto (25). */
    public static Partida crear(List<String> apodos) {
        return crear(apodos, null);
    }

    /** Instantánea inmutable del estado público de la partida. */
    public EstadoPartida estado() {
        return estado;
    }

    /** Órdenes pendientes de la ronda actual por jugador; un jugador ausente no ha enviado órdenes. */
    public Map<String, List<Orden>> ordenesPendientes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(ordenesPendientes));
    }

    /**
     * Envía las órdenes de un jugador para la ronda actual, sustituyendo por completo las anteriores.
     * Un conjunto vacío es válido y significa no mover nada.
     *
     * @throws ReglaInfringidaException si alguna orden no es válida (no se aplica ninguna), si el jugador
     *                                  está eliminado o si la partida ha terminado
     */
    public void enviarOrdenes(String jugador, List<Orden> ordenes) {
        ValidadorOrdenes.validar(estado, jugador, ordenes);
        ordenesPendientes.put(jugador, List.copyOf(ordenes));
    }

    /**
     * Resuelve la ronda actual con las órdenes pendientes y las vacía.
     *
     * @throws ReglaInfringidaException si la partida ha terminado
     */
    public InformeRonda resolverRonda() {
        ResultadoRonda resultado = ResolutorRonda.resolver(estado, ordenesPendientes);
        estado = resultado.estado();
        ordenesPendientes.clear();
        return resultado.informe();
    }

    /**
     * Elimina a un jugador activo por desconexión: descarta sus órdenes pendientes, deja sus islas
     * neutrales con 0 barcos y hace desaparecer sus flotas. Si solo queda un jugador activo, la partida
     * termina de inmediato con ese jugador como ganador.
     *
     * @throws ReglaInfringidaException si la partida ha terminado o el jugador no está activo
     */
    public void eliminarPorDesconexion(String jugador) {
        if (!estado.enCurso()) {
            throw new ReglaInfringidaException("La partida ha terminado");
        }
        Jugador eliminado = estado.jugador(jugador)
                .orElseThrow(() -> new ReglaInfringidaException("El jugador '%s' no está en la partida".formatted(jugador)));
        if (eliminado.eliminado()) {
            throw new ReglaInfringidaException("El jugador '%s' está eliminado".formatted(jugador));
        }

        ordenesPendientes.remove(jugador);
        Map<IslaId, EstadoIsla> islas = new EnumMap<>(estado.islas());
        islas.replaceAll((id, isla) -> isla.esDe(jugador) ? EstadoIsla.neutral(id) : isla);
        List<Jugador> jugadores = estado.jugadores().stream()
                .map(j -> j.apodo().equals(jugador) ? j.comoEliminado() : j)
                .toList();
        List<String> activos = jugadores.stream().filter(Jugador::activo).map(Jugador::apodo).toList();
        boolean terminada = activos.size() == 1;

        estado = new EstadoPartida(
                estado.ronda(),
                estado.limiteRondas(),
                !terminada,
                jugadores,
                islas,
                estado.flotas().stream().filter(f -> !f.dueno().equals(jugador)).toList(),
                terminada ? activos : List.of());
        if (terminada) {
            ordenesPendientes.clear();
        }
    }
}
