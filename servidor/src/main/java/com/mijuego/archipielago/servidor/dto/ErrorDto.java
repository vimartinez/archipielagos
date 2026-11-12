package com.mijuego.archipielago.servidor.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Petición rechazada.
 *
 * @param mensaje  motivo legible, en español
 * @param peticion tipo del mensaje que se rechazó (p. ej. {@code "ordenes"}), o null si no se pudo leer
 */
@JsonTypeName("error")
public record ErrorDto(String mensaje, String peticion) implements MensajeSaliente {
}
