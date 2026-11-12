package com.mijuego.archipielago.servidor.sala;

import com.mijuego.archipielago.servidor.dto.MensajeSaliente;
import java.util.ArrayList;
import java.util.List;

/** Doble de test: graba los mensajes enviados a cada conexión. */
final class NotificadorGrabador implements Notificador {

    record Envio(String conexion, MensajeSaliente mensaje) {
    }

    private final List<Envio> envios = new ArrayList<>();

    @Override
    public synchronized void enviar(String conexion, MensajeSaliente mensaje) {
        envios.add(new Envio(conexion, mensaje));
    }

    synchronized List<Envio> envios() {
        return List.copyOf(envios);
    }

    /** Mensajes de un tipo recibidos por una conexión, en orden. */
    synchronized <T extends MensajeSaliente> List<T> recibidos(String conexion, Class<T> tipo) {
        return envios.stream()
                .filter(e -> e.conexion().equals(conexion) && tipo.isInstance(e.mensaje()))
                .map(e -> tipo.cast(e.mensaje()))
                .toList();
    }

    /** Último mensaje de un tipo recibido por una conexión; falla si no hay ninguno. */
    <T extends MensajeSaliente> T ultimo(String conexion, Class<T> tipo) {
        List<T> recibidos = recibidos(conexion, tipo);
        if (recibidos.isEmpty()) {
            throw new AssertionError("'%s' no ha recibido ningún %s".formatted(conexion, tipo.getSimpleName()));
        }
        return recibidos.getLast();
    }

    synchronized void limpiar() {
        envios.clear();
    }
}
