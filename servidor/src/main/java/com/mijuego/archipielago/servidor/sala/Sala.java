package com.mijuego.archipielago.servidor.sala;

import com.mijuego.archipielago.motor.Partida;
import com.mijuego.archipielago.motor.modelo.ConfiguracionPartida;
import com.mijuego.archipielago.motor.modelo.Jugador;
import com.mijuego.archipielago.motor.modelo.Orden;
import com.mijuego.archipielago.motor.resolucion.InformeRonda;
import com.mijuego.archipielago.servidor.dto.InformeDto;
import com.mijuego.archipielago.servidor.dto.MapaDto;
import com.mijuego.archipielago.servidor.dto.OrdenDto;
import com.mijuego.archipielago.servidor.dto.OrdenesAceptadasDto;
import com.mijuego.archipielago.servidor.dto.PartidaDto;
import com.mijuego.archipielago.servidor.dto.SalaDto;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Una sala y, cuando empieza, su partida en línea.
 *
 * <p>Todos los eventos (mensajes de jugadores, desconexiones y vencimientos del plazo) entran por
 * métodos {@code synchronized}, de modo que la {@link Partida} del motor, que no es thread-safe,
 * nunca se usa desde dos hilos a la vez.
 *
 * <p>El anfitrión es siempre el participante que lleva más tiempo en la sala (el primero de la lista),
 * así que el relevo al irse el anfitrión es automático.
 */
final class Sala {

    enum Fase { EN_ESPERA, EN_PARTIDA, TERMINADA }

    static final int MAX_JUGADORES = ConfiguracionPartida.MAX_JUGADORES;

    private record Participante(String conexion, String apodo) {
    }

    private final String codigo;
    private final Notificador notificador;
    private final Planificador planificador;
    private final Duration duracionRonda;
    private final Consumer<Sala> alCerrarse;

    private final List<Participante> participantes = new ArrayList<>();
    private final Set<String> confirmados = new LinkedHashSet<>();
    private Fase fase = Fase.EN_ESPERA;
    private boolean cerrada;
    private int limiteRondas = ConfiguracionPartida.LIMITE_RONDAS_POR_DEFECTO;
    private Partida partida;
    private Planificador.Tarea plazo;
    private Instant vence;

    Sala(String codigo, Notificador notificador, Planificador planificador, Duration duracionRonda,
         Consumer<Sala> alCerrarse) {
        this.codigo = codigo;
        this.notificador = notificador;
        this.planificador = planificador;
        this.duracionRonda = duracionRonda;
        this.alCerrarse = alCerrarse;
    }

    String codigo() {
        return codigo;
    }

    synchronized Fase fase() {
        return fase;
    }

    synchronized boolean cerrada() {
        return cerrada;
    }

    // ---------------------------------------------------------------- sala de espera

    synchronized void unirse(String conexion, String apodo) {
        if (cerrada) {
            throw new PeticionRechazadaException("La sala no existe");
        }
        if (fase != Fase.EN_ESPERA) {
            throw new PeticionRechazadaException("La partida ya ha empezado");
        }
        if (participantes.size() >= MAX_JUGADORES) {
            throw new PeticionRechazadaException("La sala está llena");
        }
        String clave = apodo.toLowerCase(Locale.ROOT);
        if (participantes.stream().anyMatch(p -> p.apodo().toLowerCase(Locale.ROOT).equals(clave))) {
            throw new PeticionRechazadaException("El apodo '%s' ya está en uso en la sala".formatted(apodo));
        }
        participantes.add(new Participante(conexion, apodo));
        difundirSala();
    }

    synchronized void configurarLimite(String conexion, Integer limite) {
        exigirEnEspera();
        exigirAnfitrion(conexion, "Solo el anfitrión puede cambiar el límite de rondas");
        if (limite == null || limite < ConfiguracionPartida.MIN_LIMITE_RONDAS
                || limite > ConfiguracionPartida.MAX_LIMITE_RONDAS) {
            throw new PeticionRechazadaException("El límite de rondas debe estar entre %d y %d"
                    .formatted(ConfiguracionPartida.MIN_LIMITE_RONDAS, ConfiguracionPartida.MAX_LIMITE_RONDAS));
        }
        limiteRondas = limite;
        difundirSala();
    }

    synchronized void empezar(String conexion) {
        exigirEnEspera();
        exigirAnfitrion(conexion, "Solo el anfitrión puede empezar la partida");
        if (participantes.size() < ConfiguracionPartida.MIN_JUGADORES) {
            throw new PeticionRechazadaException("Se necesitan al menos %d jugadores"
                    .formatted(ConfiguracionPartida.MIN_JUGADORES));
        }
        partida = Partida.crear(participantes.stream().map(Participante::apodo).toList(), limiteRondas);
        fase = Fase.EN_PARTIDA;
        MapaDto mapa = MapaDto.delMapa();
        participantes.forEach(p -> notificador.enviar(p.conexion(), mapa));
        iniciarRonda();
        difundirPartida();
    }

    // ---------------------------------------------------------------- partida

    synchronized void enviarOrdenes(String conexion, List<Orden> ordenes) {
        String apodo = exigirEnPartida(conexion);
        if (confirmados.contains(apodo)) {
            throw new PeticionRechazadaException("Ya confirmaste tus órdenes para esta ronda");
        }
        partida.enviarOrdenes(apodo, ordenes);
        notificador.enviar(conexion, new OrdenesAceptadasDto(ordenes.stream().map(OrdenDto::de).toList()));
    }

    synchronized void confirmar(String conexion) {
        String apodo = exigirEnPartida(conexion);
        if (!esActivo(apodo)) {
            throw new PeticionRechazadaException("Estás eliminado: no puedes confirmar órdenes");
        }
        if (!confirmados.add(apodo)) {
            throw new PeticionRechazadaException("Ya confirmaste tus órdenes para esta ronda");
        }
        if (todosConfirmados()) {
            resolverRonda();
        } else {
            difundirPartida();
        }
    }

    /** Vencimiento del plazo de {@code ronda}; no hace nada si esa ronda ya se resolvió. */
    synchronized void plazoVencido(int ronda) {
        if (fase == Fase.EN_PARTIDA && partida.estado().ronda() == ronda) {
            resolverRonda();
        }
    }

    // ---------------------------------------------------------------- desconexión

    synchronized void desconectar(String conexion) {
        Optional<Participante> saliente = participante(conexion);
        if (saliente.isEmpty()) {
            return;
        }
        participantes.remove(saliente.get());
        if (participantes.isEmpty()) {
            cerrar();
            return;
        }
        switch (fase) {
            case EN_ESPERA -> difundirSala();
            case EN_PARTIDA -> desconectarEnPartida(saliente.get().apodo());
            case TERMINADA -> { }
        }
    }

    private void desconectarEnPartida(String apodo) {
        if (!esActivo(apodo)) {
            return; // un espectador que se va no afecta a la partida
        }
        partida.eliminarPorDesconexion(apodo);
        confirmados.remove(apodo);
        if (!partida.estado().enCurso()) {
            terminar();
            difundirPartida();
        } else if (todosConfirmados()) {
            resolverRonda();
        } else {
            difundirPartida();
        }
    }

    // ---------------------------------------------------------------- rondas

    private void iniciarRonda() {
        int ronda = partida.estado().ronda();
        vence = planificador.ahora().plus(duracionRonda);
        plazo = planificador.programar(duracionRonda, () -> plazoVencido(ronda));
    }

    private void resolverRonda() {
        cancelarPlazo();
        InformeRonda informe = partida.resolverRonda();
        confirmados.clear();
        if (partida.estado().enCurso()) {
            iniciarRonda();
        } else {
            terminar();
        }
        InformeDto dto = InformeDto.de(informe);
        participantes.forEach(p -> notificador.enviar(p.conexion(), dto));
        difundirPartida();
    }

    private boolean todosConfirmados() {
        List<Jugador> activos = partida.estado().jugadoresActivos();
        return !activos.isEmpty() && activos.stream().allMatch(j -> confirmados.contains(j.apodo()));
    }

    private void terminar() {
        fase = Fase.TERMINADA;
        cancelarPlazo();
        vence = null;
    }

    private void cerrar() {
        cerrada = true;
        cancelarPlazo();
        alCerrarse.accept(this);
    }

    private void cancelarPlazo() {
        if (plazo != null) {
            plazo.cancelar();
            plazo = null;
        }
    }

    // ---------------------------------------------------------------- difusión

    private void difundirSala() {
        List<String> apodos = participantes.stream().map(Participante::apodo).toList();
        String anfitrion = apodos.getFirst();
        participantes.forEach(p -> notificador.enviar(p.conexion(),
                new SalaDto(codigo, p.apodo(), apodos, anfitrion, limiteRondas)));
    }

    private void difundirPartida() {
        long msRestantes = vence == null ? 0
                : Math.max(0, Duration.between(planificador.ahora(), vence).toMillis());
        participantes.forEach(p -> notificador.enviar(p.conexion(),
                PartidaDto.de(partida.estado(), p.apodo(), msRestantes, confirmados)));
    }

    // ---------------------------------------------------------------- utilidades

    private Optional<Participante> participante(String conexion) {
        return participantes.stream().filter(p -> p.conexion().equals(conexion)).findFirst();
    }

    private String apodoDe(String conexion) {
        return participante(conexion)
                .orElseThrow(() -> new PeticionRechazadaException("No estás en esta sala"))
                .apodo();
    }

    private boolean esActivo(String apodo) {
        return partida.estado().jugador(apodo).map(Jugador::activo).orElse(false);
    }

    private void exigirEnEspera() {
        if (fase != Fase.EN_ESPERA) {
            throw new PeticionRechazadaException("La partida ya ha empezado");
        }
    }

    private void exigirAnfitrion(String conexion, String mensaje) {
        apodoDe(conexion);
        if (!participantes.getFirst().conexion().equals(conexion)) {
            throw new PeticionRechazadaException(mensaje);
        }
    }

    private String exigirEnPartida(String conexion) {
        String apodo = apodoDe(conexion);
        if (fase == Fase.EN_ESPERA) {
            throw new PeticionRechazadaException("La partida no ha empezado");
        }
        if (fase == Fase.TERMINADA) {
            throw new PeticionRechazadaException("La partida ha terminado");
        }
        return apodo;
    }
}
