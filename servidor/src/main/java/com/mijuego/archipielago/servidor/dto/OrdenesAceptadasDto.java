package com.mijuego.archipielago.servidor.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

/** Confirma a un jugador cuáles son sus órdenes vigentes en la ronda en curso. */
@JsonTypeName("ordenesAceptadas")
public record OrdenesAceptadasDto(List<OrdenDto> ordenes) implements MensajeSaliente {
}
