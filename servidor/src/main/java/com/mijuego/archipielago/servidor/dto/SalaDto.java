package com.mijuego.archipielago.servidor.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

/** Estado de una sala en espera, tal como lo ve un jugador. */
@JsonTypeName("sala")
public record SalaDto(String codigo, String tuApodo, List<String> jugadores, String anfitrion, int limiteRondas)
        implements MensajeSaliente {
}
