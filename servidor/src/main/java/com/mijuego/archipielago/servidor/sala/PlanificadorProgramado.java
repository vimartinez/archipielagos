package com.mijuego.archipielago.servidor.sala;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Planificador real: reloj del sistema y un {@link ScheduledExecutorService} compartido. */
public final class PlanificadorProgramado implements Planificador {

    private static final Logger LOG = LoggerFactory.getLogger(PlanificadorProgramado.class);

    private final ScheduledExecutorService ejecutor;
    private final Clock reloj;

    public PlanificadorProgramado(ScheduledExecutorService ejecutor, Clock reloj) {
        this.ejecutor = ejecutor;
        this.reloj = reloj;
    }

    @Override
    public Instant ahora() {
        return reloj.instant();
    }

    @Override
    public Tarea programar(Duration retraso, Runnable accion) {
        ScheduledFuture<?> futuro = ejecutor.schedule(() -> {
            try {
                accion.run();
            } catch (RuntimeException e) {
                LOG.error("Fallo en una tarea programada", e);
            }
        }, retraso.toMillis(), TimeUnit.MILLISECONDS);
        return () -> futuro.cancel(false);
    }
}
