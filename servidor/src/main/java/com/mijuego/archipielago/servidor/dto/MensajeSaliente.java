package com.mijuego.archipielago.servidor.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Mensaje que el servidor envía a un cliente. Se serializa como JSON con un campo {@code tipo}. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tipo")
public sealed interface MensajeSaliente
        permits SalaDto, MapaDto, PartidaDto, InformeDto, OrdenesAceptadasDto, ErrorDto {
}
