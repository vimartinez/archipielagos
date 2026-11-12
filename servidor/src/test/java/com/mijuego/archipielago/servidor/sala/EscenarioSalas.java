package com.mijuego.archipielago.servidor.sala;

import com.mijuego.archipielago.servidor.dto.ErrorDto;
import com.mijuego.archipielago.servidor.dto.OrdenDto;
import com.mijuego.archipielago.servidor.dto.PartidaDto;
import com.mijuego.archipielago.servidor.dto.SalaDto;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Base de los tests del núcleo: un gestor con dobles de test. Cada jugador se conecta con su apodo
 * en minúsculas como identificador de conexión ("Ana" usa la conexión "ana").
 */
abstract class EscenarioSalas {

    static final Duration RONDA = Duration.ofSeconds(60);

    final NotificadorGrabador notificador = new NotificadorGrabador();
    final PlanificadorManual planificador = new PlanificadorManual();
    final GestorSalas gestor = new GestorSalas(notificador, planificador, RONDA);

    static String conexion(String apodo) {
        return apodo.toLowerCase(Locale.ROOT);
    }

    /** Crea una sala con el primer apodo como anfitrión y une al resto; devuelve el código. */
    String sala(String anfitrion, String... invitados) {
        gestor.crearSala(conexion(anfitrion), anfitrion);
        String codigo = notificador.ultimo(conexion(anfitrion), SalaDto.class).codigo();
        for (String invitado : invitados) {
            gestor.unirse(conexion(invitado), codigo, invitado);
        }
        return codigo;
    }

    /** Crea la sala, une a los demás y empieza la partida. */
    String partida(String anfitrion, String... invitados) {
        String codigo = sala(anfitrion, invitados);
        gestor.empezar(conexion(anfitrion));
        return codigo;
    }

    PartidaDto estado(String apodo) {
        return notificador.ultimo(conexion(apodo), PartidaDto.class);
    }

    SalaDto vistaSala(String apodo) {
        return notificador.ultimo(conexion(apodo), SalaDto.class);
    }

    ErrorDto error(String apodo) {
        return notificador.ultimo(conexion(apodo), ErrorDto.class);
    }

    int errores(String apodo) {
        return notificador.recibidos(conexion(apodo), ErrorDto.class).size();
    }

    void ordenes(String apodo, OrdenDto... ordenes) {
        gestor.enviarOrdenes(conexion(apodo), List.of(ordenes));
    }

    void confirmar(String... apodos) {
        for (String apodo : apodos) {
            gestor.confirmar(conexion(apodo));
        }
    }

    static OrdenDto orden(int barcos, String origen, String destino) {
        return new OrdenDto(origen, destino, barcos);
    }

    PartidaDto.IslaDto isla(String apodo, String id) {
        return estado(apodo).islas().stream().filter(i -> i.id().equals(id)).findFirst().orElseThrow();
    }
}
