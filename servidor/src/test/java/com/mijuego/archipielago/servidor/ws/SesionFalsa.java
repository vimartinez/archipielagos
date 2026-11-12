package com.mijuego.archipielago.servidor.ws;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/** Doble de test de una sesión WebSocket: graba lo enviado y si se cerró. */
final class SesionFalsa implements WebSocketSession {

    private final String id;
    private final List<WebSocketMessage<?>> enviados = new ArrayList<>();
    private CloseStatus cierre;

    SesionFalsa(String id) {
        this.id = id;
    }

    synchronized List<String> textos() {
        return enviados.stream().filter(m -> m instanceof TextMessage).map(m -> ((TextMessage) m).getPayload()).toList();
    }

    synchronized long pings() {
        return enviados.stream().filter(m -> m instanceof org.springframework.web.socket.PingMessage).count();
    }

    synchronized CloseStatus cierre() {
        return cierre;
    }

    @Override public String getId() { return id; }
    @Override public URI getUri() { return null; }
    @Override public HttpHeaders getHandshakeHeaders() { return new HttpHeaders(); }
    @Override public Map<String, Object> getAttributes() { return new HashMap<>(); }
    @Override public Principal getPrincipal() { return null; }
    @Override public InetSocketAddress getLocalAddress() { return null; }
    @Override public InetSocketAddress getRemoteAddress() { return null; }
    @Override public String getAcceptedProtocol() { return null; }
    @Override public void setTextMessageSizeLimit(int limite) { }
    @Override public int getTextMessageSizeLimit() { return 0; }
    @Override public void setBinaryMessageSizeLimit(int limite) { }
    @Override public int getBinaryMessageSizeLimit() { return 0; }
    @Override public List<WebSocketExtension> getExtensions() { return List.of(); }
    @Override public synchronized void sendMessage(WebSocketMessage<?> mensaje) { enviados.add(mensaje); }
    @Override public synchronized boolean isOpen() { return cierre == null; }
    @Override public void close() { close(CloseStatus.NORMAL); }
    @Override public synchronized void close(CloseStatus estado) { cierre = estado; }
}
