package com.mijuego.archipielago.servidor.sala;

import com.mijuego.archipielago.motor.ReglaInfringidaException;
import com.mijuego.archipielago.motor.mapa.IslaId;
import com.mijuego.archipielago.motor.modelo.Orden;
import com.mijuego.archipielago.servidor.dto.ErrorDto;
import com.mijuego.archipielago.servidor.dto.OrdenDto;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Registro de salas y punto de entrada de todos los comandos de los jugadores.
 *
 * <p>Cada jugador se identifica por su conexión. Las peticiones rechazadas no lanzan excepciones al
 * llamante: se responden con un mensaje {@link ErrorDto} solo a la conexión que las hizo.
 */
public final class GestorSalas {

    public static final int MAX_LONGITUD_APODO = 20;
    private static final int MAX_INTENTOS_CODIGO = 100;

    private final Notificador notificador;
    private final Planificador planificador;
    private final Duration duracionRonda;
    private final Supplier<String> generadorCodigos;

    private final ConcurrentMap<String, Sala> salas = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Sala> salaDeConexion = new ConcurrentHashMap<>();

    public GestorSalas(Notificador notificador, Planificador planificador, Duration duracionRonda,
                       Supplier<String> generadorCodigos) {
        this.notificador = notificador;
        this.planificador = planificador;
        this.duracionRonda = duracionRonda;
        this.generadorCodigos = generadorCodigos;
    }

    public GestorSalas(Notificador notificador, Planificador planificador, Duration duracionRonda) {
        this(notificador, planificador, duracionRonda, new GeneradorCodigos());
    }

    public void crearSala(String conexion, String apodo) {
        atender(conexion, "crearSala", () -> {
            exigirSinSala(conexion);
            String valido = validarApodo(apodo);
            Sala sala = registrarSalaNueva();
            sala.unirse(conexion, valido);
            salaDeConexion.put(conexion, sala);
        });
    }

    public void unirse(String conexion, String codigo, String apodo) {
        atender(conexion, "unirse", () -> {
            exigirSinSala(conexion);
            String valido = validarApodo(apodo);
            String normalizado = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
            Sala sala = Optional.ofNullable(salas.get(normalizado))
                    .orElseThrow(() -> new PeticionRechazadaException("La sala no existe"));
            sala.unirse(conexion, valido);
            salaDeConexion.put(conexion, sala);
        });
    }

    public void configurarLimite(String conexion, Integer limiteRondas) {
        atender(conexion, "configurarLimite", () -> salaDe(conexion).configurarLimite(conexion, limiteRondas));
    }

    public void empezar(String conexion) {
        atender(conexion, "empezar", () -> salaDe(conexion).empezar(conexion));
    }

    public void enviarOrdenes(String conexion, List<OrdenDto> ordenes) {
        atender(conexion, "ordenes", () -> salaDe(conexion).enviarOrdenes(conexion, convertir(ordenes)));
    }

    public void confirmar(String conexion) {
        atender(conexion, "confirmar", () -> salaDe(conexion).confirmar(conexion));
    }

    /** La conexión se ha cerrado. Es idempotente: una conexión desconocida se ignora. */
    public void desconectar(String conexion) {
        Sala sala = salaDeConexion.remove(conexion);
        if (sala != null) {
            sala.desconectar(conexion);
        }
    }

    /** Indica si existe una sala abierta con ese código. */
    public boolean existeSala(String codigo) {
        return salas.containsKey(codigo);
    }

    public int numeroSalas() {
        return salas.size();
    }

    // ---------------------------------------------------------------- internos

    private void atender(String conexion, String peticion, Runnable accion) {
        try {
            accion.run();
        } catch (PeticionRechazadaException | ReglaInfringidaException e) {
            notificador.enviar(conexion, new ErrorDto(e.getMessage(), peticion));
        }
    }

    private Sala registrarSalaNueva() {
        for (int i = 0; i < MAX_INTENTOS_CODIGO; i++) {
            String codigo = generadorCodigos.get();
            Sala sala = new Sala(codigo, notificador, planificador, duracionRonda, s -> salas.remove(s.codigo(), s));
            if (salas.putIfAbsent(codigo, sala) == null) {
                return sala;
            }
        }
        throw new IllegalStateException("No se pudo generar un código de sala libre");
    }

    private void exigirSinSala(String conexion) {
        if (salaDeConexion.containsKey(conexion)) {
            throw new PeticionRechazadaException("Ya estás en una sala");
        }
    }

    private Sala salaDe(String conexion) {
        Sala sala = salaDeConexion.get(conexion);
        if (sala == null) {
            throw new PeticionRechazadaException("No estás en ninguna sala");
        }
        return sala;
    }

    static String validarApodo(String apodo) {
        String recortado = apodo == null ? "" : apodo.strip();
        if (recortado.isEmpty() || recortado.length() > MAX_LONGITUD_APODO) {
            throw new PeticionRechazadaException("El apodo debe tener entre 1 y %d caracteres"
                    .formatted(MAX_LONGITUD_APODO));
        }
        return recortado;
    }

    private static List<Orden> convertir(List<OrdenDto> ordenes) {
        if (ordenes == null) {
            return List.of();
        }
        return ordenes.stream().map(o -> new Orden(isla(o.origen()), isla(o.destino()), o.barcos())).toList();
    }

    private static IslaId isla(String id) {
        try {
            return IslaId.valueOf(id);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new PeticionRechazadaException("Isla desconocida: " + id);
        }
    }
}
