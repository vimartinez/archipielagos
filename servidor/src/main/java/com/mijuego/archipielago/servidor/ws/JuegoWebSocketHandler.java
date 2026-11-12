package com.mijuego.archipielago.servidor.ws;

import com.mijuego.archipielago.servidor.dto.ErrorDto;
import com.mijuego.archipielago.servidor.dto.MensajeEntrante;
import com.mijuego.archipielago.servidor.sala.GestorSalas;
import com.mijuego.archipielago.servidor.sala.Planificador;
import java.time.Duration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptador WebSocket del juego: traduce los mensajes JSON de los clientes a comandos del
 * {@link GestorSalas} e informa de las conexiones que se cierran.
 */
public class JuegoWebSocketHandler extends AbstractWebSocketHandler {

    /** Tamaño máximo de un mensaje entrante, en bytes. */
    public static final int MAX_BYTES_MENSAJE = 8 * 1024;

    private final JsonMapper json;
    private final SesionesWebSocket sesiones;
    private final GestorSalas gestor;
    private final Planificador planificador;
    private final Duration limitePong;

    public JuegoWebSocketHandler(JsonMapper json, SesionesWebSocket sesiones, GestorSalas gestor,
                                 Planificador planificador, Duration limitePong) {
        this.json = json;
        this.sesiones = sesiones;
        this.gestor = gestor;
        this.planificador = planificador;
        this.limitePong = limitePong;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession sesion) {
        sesiones.registrar(sesion, planificador.ahora());
    }

    @Override
    protected void handleTextMessage(WebSocketSession sesion, TextMessage mensaje) {
        String id = sesion.getId();
        if (mensaje.getPayloadLength() > MAX_BYTES_MENSAJE) {
            sesiones.enviar(id, new ErrorDto("Mensaje demasiado grande", null));
            return;
        }
        MensajeEntrante entrante;
        try {
            entrante = json.readValue(mensaje.getPayload(), MensajeEntrante.class);
        } catch (JacksonException e) {
            sesiones.enviar(id, new ErrorDto("Mensaje no válido", null));
            return;
        }
        switch (entrante) {
            case MensajeEntrante.CrearSala m -> gestor.crearSala(id, m.apodo());
            case MensajeEntrante.Unirse m -> gestor.unirse(id, m.codigo(), m.apodo());
            case MensajeEntrante.ConfigurarLimite m -> gestor.configurarLimite(id, m.limiteRondas());
            case MensajeEntrante.Empezar m -> gestor.empezar(id);
            case MensajeEntrante.Ordenes m -> gestor.enviarOrdenes(id, m.ordenes());
            case MensajeEntrante.Confirmar m -> gestor.confirmar(id);
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession sesion, PongMessage mensaje) {
        sesiones.pong(sesion.getId(), planificador.ahora());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sesion, CloseStatus estado) {
        sesiones.quitar(sesion.getId());
        gestor.desconectar(sesion.getId());
    }

    /** Envía pings y elimina a los jugadores cuyas conexiones han dejado de responder. */
    public void latir() {
        sesiones.latir(planificador.ahora(), limitePong).forEach(gestor::desconectar);
    }
}
