package com.mijuego.archipielago.servidor.sala;

import java.time.Duration;
import java.time.Instant;

/** Puerto de tiempo del núcleo: da la hora actual y programa tareas diferidas. */
public interface Planificador {

    Instant ahora();

    /** Programa {@code accion} para que se ejecute una vez pasado {@code retraso}. */
    Tarea programar(Duration retraso, Runnable accion);

    /** Tarea programada que todavía puede cancelarse. */
    interface Tarea {
        void cancelar();
    }
}
