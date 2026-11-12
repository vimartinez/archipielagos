package com.mijuego.archipielago.servidor.ws;

import static org.assertj.core.api.Assertions.assertThat;

import com.mijuego.archipielago.servidor.sala.GestorSalas;
import com.mijuego.archipielago.servidor.sala.PlanificadorManual;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class JuegoWebSocketHandlerTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final PlanificadorManual planificador = new PlanificadorManual();
    private final SesionesWebSocket sesiones = new SesionesWebSocket(json);
    private final GestorSalas gestor = new GestorSalas(sesiones, planificador, Duration.ofSeconds(60), () -> "AAAA");
    private final JuegoWebSocketHandler handler =
            new JuegoWebSocketHandler(json, sesiones, gestor, planificador, Duration.ofSeconds(60));

    private final SesionFalsa ana = new SesionFalsa("a");
    private final SesionFalsa beto = new SesionFalsa("b");

    private void recibir(SesionFalsa sesion, String texto) throws Exception {
        handler.handleMessage(sesion, new TextMessage(texto));
    }

    private JsonNode ultimo(SesionFalsa sesion) {
        return json.readTree(sesion.textos().getLast());
    }

    @Test
    void tipoDesconocidoRecibeUnError() throws Exception {
        handler.afterConnectionEstablished(ana);
        recibir(ana, "{\"tipo\":\"bailar\"}");

        assertThat(ultimo(ana).get("tipo").asString()).isEqualTo("error");
        assertThat(ultimo(ana).get("mensaje").asString()).isEqualTo("Mensaje no válido");
    }

    @Test
    void jsonRotoRecibeUnError() throws Exception {
        handler.afterConnectionEstablished(ana);
        recibir(ana, "{no es json");

        assertThat(ultimo(ana).get("mensaje").asString()).isEqualTo("Mensaje no válido");
    }

    @Test
    void mensajeDemasiadoGrande() throws Exception {
        handler.afterConnectionEstablished(ana);
        recibir(ana, "{\"tipo\":\"crearSala\",\"apodo\":\"" + "x".repeat(9000) + "\"}");

        assertThat(ultimo(ana).get("mensaje").asString()).isEqualTo("Mensaje demasiado grande");
        assertThat(gestor.numeroSalas()).isZero();
    }

    @Test
    void traduceLosMensajesAComandos() throws Exception {
        handler.afterConnectionEstablished(ana);
        handler.afterConnectionEstablished(beto);
        recibir(ana, "{\"tipo\":\"crearSala\",\"apodo\":\"Ana\"}");
        recibir(beto, "{\"tipo\":\"unirse\",\"codigo\":\"aaaa\",\"apodo\":\"Beto\"}");
        recibir(ana, "{\"tipo\":\"configurarLimite\",\"limiteRondas\":12}");
        recibir(ana, "{\"tipo\":\"empezar\"}");
        recibir(ana, "{\"tipo\":\"ordenes\",\"ordenes\":[{\"origen\":\"S1\",\"destino\":\"I1\",\"barcos\":2}]}");
        assertThat(ultimo(ana).get("tipo").asString()).isEqualTo("ordenesAceptadas");
        recibir(ana, "{\"tipo\":\"confirmar\"}");

        JsonNode estado = ultimo(beto);
        assertThat(estado.get("tipo").asString()).isEqualTo("partida");
        assertThat(estado.get("limiteRondas").asInt()).isEqualTo(12);
        assertThat(estado.get("confirmados").get(0).asString()).isEqualTo("Ana");
    }

    @Test
    void cerrarLaConexionEliminaAlJugador() throws Exception {
        handler.afterConnectionEstablished(ana);
        handler.afterConnectionEstablished(beto);
        recibir(ana, "{\"tipo\":\"crearSala\",\"apodo\":\"Ana\"}");
        recibir(beto, "{\"tipo\":\"unirse\",\"codigo\":\"AAAA\",\"apodo\":\"Beto\"}");
        recibir(ana, "{\"tipo\":\"empezar\"}");

        handler.afterConnectionClosed(beto, CloseStatus.GOING_AWAY);

        JsonNode fin = ultimo(ana);
        assertThat(fin.get("enCurso").asBoolean()).isFalse();
        assertThat(fin.get("ganadores").get(0).asString()).isEqualTo("Ana");
    }

    @Test
    void unaSesionSinPongSeCierraYSuJugadorQuedaEliminado() throws Exception {
        handler.afterConnectionEstablished(ana);
        handler.afterConnectionEstablished(beto);
        recibir(ana, "{\"tipo\":\"crearSala\",\"apodo\":\"Ana\"}");
        recibir(beto, "{\"tipo\":\"unirse\",\"codigo\":\"AAAA\",\"apodo\":\"Beto\"}");
        recibir(ana, "{\"tipo\":\"empezar\"}");

        planificador.avanzar(Duration.ofSeconds(25));
        handler.latir();
        assertThat(ana.pings()).isEqualTo(1);
        assertThat(beto.pings()).isEqualTo(1);

        planificador.avanzar(Duration.ofSeconds(25));
        handler.handleMessage(ana, new PongMessage());
        planificador.avanzar(Duration.ofSeconds(11)); // Beto lleva 61 s sin pong; Ana, 11 s
        handler.latir();

        assertThat(beto.cierre()).isEqualTo(CloseStatus.SESSION_NOT_RELIABLE);
        assertThat(ana.cierre()).isNull();
        JsonNode fin = ultimo(ana);
        assertThat(fin.get("enCurso").asBoolean()).isFalse();
        assertThat(fin.get("ganadores").get(0).asString()).isEqualTo("Ana");

        // el cierre que notifica después el contenedor no tiene más efecto
        handler.afterConnectionClosed(beto, CloseStatus.SESSION_NOT_RELIABLE);
        assertThat(ultimo(ana)).isEqualTo(fin);
    }
}
