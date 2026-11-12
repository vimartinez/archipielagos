package com.mijuego.archipielago.servidor;

import com.mijuego.archipielago.servidor.sala.GestorSalas;
import com.mijuego.archipielago.servidor.sala.Planificador;
import com.mijuego.archipielago.servidor.sala.PlanificadorProgramado;
import com.mijuego.archipielago.servidor.ws.JuegoWebSocketHandler;
import com.mijuego.archipielago.servidor.ws.SesionesWebSocket;
import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import tools.jackson.databind.json.JsonMapper;

/** Monta el núcleo de salas, el WebSocket en {@code /ws} y el latido de las conexiones. */
@Configuration
@EnableWebSocket
public class ConfiguracionJuego {

    @Bean(destroyMethod = "shutdownNow")
    ScheduledExecutorService temporizador() {
        return Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("temporizador").daemon().factory());
    }

    @Bean
    Planificador planificador(ScheduledExecutorService temporizador) {
        return new PlanificadorProgramado(temporizador, Clock.systemUTC());
    }

    @Bean
    SesionesWebSocket sesionesWebSocket(JsonMapper json) {
        return new SesionesWebSocket(json);
    }

    @Bean
    GestorSalas gestorSalas(SesionesWebSocket sesiones, Planificador planificador, PropiedadesJuego propiedades) {
        return new GestorSalas(sesiones, planificador, propiedades.duracionRonda());
    }

    @Bean
    JuegoWebSocketHandler juegoWebSocketHandler(JsonMapper json, SesionesWebSocket sesiones, GestorSalas gestor,
                                                Planificador planificador, PropiedadesJuego propiedades,
                                                ScheduledExecutorService temporizador) {
        JuegoWebSocketHandler handler = new JuegoWebSocketHandler(json, sesiones, gestor, planificador,
                propiedades.limitePong());
        long intervalo = propiedades.intervaloPing().toMillis();
        temporizador.scheduleAtFixedRate(() -> {
            try {
                handler.latir();
            } catch (RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger(ConfiguracionJuego.class).error("Fallo en el latido", e);
            }
        }, intervalo, intervalo, TimeUnit.MILLISECONDS);
        return handler;
    }

    @Bean
    WebSocketConfigurer registroWebSocket(JuegoWebSocketHandler handler) {
        return (WebSocketHandlerRegistry registro) -> registro.addHandler(handler, "/ws");
    }
}
