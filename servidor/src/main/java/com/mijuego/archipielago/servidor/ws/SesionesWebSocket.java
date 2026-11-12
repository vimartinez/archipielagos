package com.mijuego.archipielago.servidor.ws;

import com.mijuego.archipielago.servidor.dto.MensajeSaliente;
import com.mijuego.archipielago.servidor.sala.Notificador;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.json.JsonMapper;

/**
 * Conexiones WebSocket abiertas, indexadas por el id de sesión, que es la identidad del jugador.
 * Implementa el {@link Notificador} del núcleo serializando los mensajes a JSON.
 */
public class SesionesWebSocket implements Notificador {

    private static final Logger LOG = LoggerFactory.getLogger(SesionesWebSocket.class);
    private static final int LIMITE_TIEMPO_ENVIO_MS = 5_000;
    private static final int LIMITE_BUFFER_ENVIO = 512 * 1024;

    private static final class Conexion {
        final WebSocketSession sesion;
        volatile Instant ultimoPong;

        Conexion(WebSocketSession sesion, Instant ahora) {
            this.sesion = sesion;
            this.ultimoPong = ahora;
        }
    }

    private final JsonMapper json;
    private final ConcurrentMap<String, Conexion> conexiones = new ConcurrentHashMap<>();

    public SesionesWebSocket(JsonMapper json) {
        this.json = json;
    }

    void registrar(WebSocketSession sesion, Instant ahora) {
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(sesion, LIMITE_TIEMPO_ENVIO_MS, LIMITE_BUFFER_ENVIO);
        conexiones.put(sesion.getId(), new Conexion(segura, ahora));
    }

    void quitar(String id) {
        conexiones.remove(id);
    }

    void pong(String id, Instant ahora) {
        Conexion conexion = conexiones.get(id);
        if (conexion != null) {
            conexion.ultimoPong = ahora;
        }
    }

    @Override
    public void enviar(String id, MensajeSaliente mensaje) {
        Conexion conexion = conexiones.get(id);
        if (conexion == null) {
            return;
        }
        try {
            conexion.sesion.sendMessage(new TextMessage(json.writeValueAsString(mensaje)));
        } catch (IOException | RuntimeException e) {
            LOG.debug("No se pudo enviar a la conexión {}: {}", id, e.getMessage());
        }
    }

    /**
     * Cierra y olvida las conexiones que llevan más de {@code limitePong} sin responder, y envía un ping
     * a las demás.
     *
     * @return ids de las conexiones cerradas
     */
    List<String> latir(Instant ahora, Duration limitePong) {
        List<String> cerradas = new ArrayList<>();
        conexiones.forEach((id, conexion) -> {
            try {
                if (Duration.between(conexion.ultimoPong, ahora).compareTo(limitePong) > 0) {
                    cerradas.add(id);
                    conexiones.remove(id);
                    conexion.sesion.close(CloseStatus.SESSION_NOT_RELIABLE);
                } else {
                    conexion.sesion.sendMessage(new PingMessage());
                }
            } catch (IOException | RuntimeException e) {
                LOG.debug("Fallo de latido en la conexión {}: {}", id, e.getMessage());
            }
        });
        return cerradas;
    }
}
