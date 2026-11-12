package com.mijuego.archipielago.servidor.dto;

import com.mijuego.archipielago.motor.modelo.Orden;

/** Una orden tal como viaja por el protocolo: islas por su identificador, p. ej. {@code "S1"}. */
public record OrdenDto(String origen, String destino, int barcos) {

    public static OrdenDto de(Orden orden) {
        return new OrdenDto(orden.origen().name(), orden.destino().name(), orden.barcos());
    }
}
