package com.mijuego.archipielago.motor.resolucion;

import com.mijuego.archipielago.motor.modelo.EstadoPartida;

/** Resultado de resolver una ronda: el nuevo estado y el informe de lo ocurrido. */
public record ResultadoRonda(EstadoPartida estado, InformeRonda informe) {
}
