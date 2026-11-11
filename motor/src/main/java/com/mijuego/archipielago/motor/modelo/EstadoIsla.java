package com.mijuego.archipielago.motor.modelo;

import com.mijuego.archipielago.motor.mapa.IslaId;
import java.util.Objects;
import java.util.Optional;

/**
 * Estado público de una isla: su dueño ({@code null} si es neutral) y los barcos que tiene.
 */
public record EstadoIsla(IslaId isla, String dueno, int barcos) {

    public EstadoIsla {
        Objects.requireNonNull(isla, "isla");
        if (barcos < 0) {
            throw new IllegalArgumentException("Los barcos de una isla no pueden ser negativos: " + barcos);
        }
    }

    /** Isla neutral con 0 barcos. */
    public static EstadoIsla neutral(IslaId isla) {
        return new EstadoIsla(isla, null, 0);
    }

    public boolean esNeutral() {
        return dueno == null;
    }

    public boolean esDe(String apodo) {
        return dueno != null && dueno.equals(apodo);
    }

    public Optional<String> duenoOpcional() {
        return Optional.ofNullable(dueno);
    }
}
