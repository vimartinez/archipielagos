package com.mijuego.archipielago.servidor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Juega una partida corta con dos clientes WebSocket reales contra el servidor arrancado. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "archipielago.duracion-ronda=2s")
class PartidaWebSocketIT {

    @LocalServerPort
    int puerto;

    @Autowired
    JsonMapper json;

    /** Un cliente que acumula los mensajes recibidos. */
    final class Cliente extends TextWebSocketHandler {
        final BlockingQueue<JsonNode> recibidos = new LinkedBlockingQueue<>();
        WebSocketSession sesion;

        Cliente conectar() throws Exception {
            sesion = new StandardWebSocketClient().execute(this, "ws://localhost:" + puerto + "/ws")
                    .get(5, TimeUnit.SECONDS);
            return this;
        }

        @Override
        protected void handleTextMessage(WebSocketSession s, TextMessage mensaje) {
            recibidos.add(json.readTree(mensaje.getPayload()));
        }

        void enviar(String texto) throws Exception {
            sesion.sendMessage(new TextMessage(texto));
        }

        /** Espera el primer mensaje de ese tipo que cumpla la condición, descartando los anteriores. */
        JsonNode esperar(String tipo, Predicate<JsonNode> condicion) throws InterruptedException {
            long limite = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            while (System.nanoTime() < limite) {
                JsonNode m = recibidos.poll(100, TimeUnit.MILLISECONDS);
                if (m != null && m.get("tipo").asString().equals(tipo) && condicion.test(m)) {
                    return m;
                }
            }
            throw new AssertionError("No llegó ningún mensaje '%s' esperado".formatted(tipo));
        }

        JsonNode esperar(String tipo) throws InterruptedException {
            return esperar(tipo, m -> true);
        }
    }

    @Test
    void partidaCortaConDosJugadores() throws Exception {
        Cliente ana = new Cliente().conectar();
        Cliente beto = new Cliente().conectar();

        ana.enviar("{\"tipo\":\"crearSala\",\"apodo\":\"Ana\"}");
        String codigo = ana.esperar("sala").get("codigo").asString();

        beto.enviar("{\"tipo\":\"unirse\",\"codigo\":\"" + codigo.toLowerCase() + "\",\"apodo\":\"Beto\"}");
        ana.esperar("sala", m -> m.get("jugadores").size() == 2);
        beto.esperar("sala", m -> m.get("jugadores").size() == 2);

        ana.enviar("{\"tipo\":\"empezar\"}");
        assertThat(beto.esperar("mapa").get("rutas")).hasSize(24);
        assertThat(ana.esperar("partida").get("ronda").asInt()).isEqualTo(1);
        beto.esperar("partida", m -> m.get("ronda").asInt() == 1);

        ana.enviar("{\"tipo\":\"ordenes\",\"ordenes\":[{\"origen\":\"S1\",\"destino\":\"I1\",\"barcos\":3}]}");
        assertThat(ana.esperar("ordenesAceptadas").get("ordenes")).hasSize(1);
        ana.enviar("{\"tipo\":\"confirmar\"}");
        beto.enviar("{\"tipo\":\"confirmar\"}");

        // resolución anticipada de la ronda 1
        assertThat(ana.esperar("informe").get("ronda").asInt()).isEqualTo(1);
        JsonNode ronda2 = beto.esperar("partida", m -> m.get("ronda").asInt() == 2);
        assertThat(ronda2.get("islas").get(8).get("dueno").asString()).isEqualTo("Ana");

        // la ronda 2 se resuelve por plazo vencido (2 s en este test)
        assertThat(beto.esperar("informe", m -> m.get("ronda").asInt() == 2)).isNotNull();
        ana.esperar("partida", m -> m.get("ronda").asInt() == 3);

        // Beto cierra la pestaña: queda eliminado y Ana gana
        beto.sesion.close(CloseStatus.GOING_AWAY);
        JsonNode fin = ana.esperar("partida", m -> !m.get("enCurso").asBoolean());
        assertThat(fin.get("ganadores")).hasSize(1);
        assertThat(fin.get("ganadores").get(0).asString()).isEqualTo("Ana");

        ana.sesion.close();
    }

    @Test
    void elServidorSirveElCliente() throws Exception {
        var respuesta = java.net.http.HttpClient.newHttpClient().send(
                java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + puerto + "/")).build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("Archipiélago");
    }
}
