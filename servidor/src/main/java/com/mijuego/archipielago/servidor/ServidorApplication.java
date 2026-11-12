package com.mijuego.archipielago.servidor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Punto de entrada del servidor de Archipiélago: sirve el cliente web y el WebSocket del juego. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ServidorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServidorApplication.class, args);
    }
}
