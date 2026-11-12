package com.mijuego.archipielago.servidor.sala;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Doble de test: un reloj que solo avanza cuando el test lo pide, ejecutando las tareas que vencen. */
public final class PlanificadorManual implements Planificador {

    private final class TareaManual implements Tarea {
        final Instant cuando;
        final Runnable accion;
        boolean cancelada;

        TareaManual(Instant cuando, Runnable accion) {
            this.cuando = cuando;
            this.accion = accion;
        }

        @Override
        public void cancelar() {
            cancelada = true;
        }
    }

    private Instant ahora = Instant.parse("2026-01-01T00:00:00Z");
    private final List<TareaManual> tareas = new ArrayList<>();

    @Override
    public Instant ahora() {
        return ahora;
    }

    @Override
    public Tarea programar(Duration retraso, Runnable accion) {
        TareaManual tarea = new TareaManual(ahora.plus(retraso), accion);
        tareas.add(tarea);
        return tarea;
    }

    /** Avanza el reloj y ejecuta, en orden, las tareas no canceladas que vencen por el camino. */
    public void avanzar(Duration tiempo) {
        Instant destino = ahora.plus(tiempo);
        while (true) {
            Optional<TareaManual> siguiente = tareas.stream()
                    .filter(t -> !t.cancelada && !t.cuando.isAfter(destino))
                    .min(Comparator.comparing(t -> t.cuando));
            if (siguiente.isEmpty()) {
                break;
            }
            TareaManual tarea = siguiente.get();
            tareas.remove(tarea);
            ahora = tarea.cuando;
            tarea.accion.run();
        }
        ahora = destino;
    }

    /** Tareas programadas que siguen pendientes (ni ejecutadas ni canceladas). */
    public long pendientes() {
        return tareas.stream().filter(t -> !t.cancelada).count();
    }
}
