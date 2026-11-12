package com.mijuego.archipielago.servidor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * En producción el servidor está detrás de un proxy HTTPS (Railway): recibe la petición por http en un
 * puerto interno, y el navegador envía {@code Origin: https://<dominio público>}. El handshake debe
 * aceptarse usando las cabeceras {@code X-Forwarded-*} del proxy, y seguir rechazando otros orígenes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrigenTrasProxyIT {

    private static final String DOMINIO = "archipielago.ejemplo.com";

    @LocalServerPort
    int puerto;

    private CompletableFuture<WebSocket> conectar(String origen) {
        return HttpClient.newHttpClient().newWebSocketBuilder()
                .header("Origin", origen)
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", DOMINIO)
                .header("X-Forwarded-Port", "443")
                .buildAsync(URI.create("ws://localhost:" + puerto + "/ws"), new WebSocket.Listener() { });
    }

    @Test
    void aceptaElOrigenPublicoTrasElProxy() throws Exception {
        WebSocket ws = conectar("https://" + DOMINIO).get(5, TimeUnit.SECONDS);
        assertThat(ws.isOutputClosed()).isFalse();
        ws.abort();
    }

    @Test
    void rechazaOtrosOrigenes() {
        assertThatThrownBy(() -> conectar("https://otro.ejemplo.com").get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasStackTraceContaining("403");
    }
}
