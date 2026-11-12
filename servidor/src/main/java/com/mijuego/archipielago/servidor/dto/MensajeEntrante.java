package com.mijuego.archipielago.servidor.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

/** Mensaje que un cliente envía al servidor, distinguido por su campo {@code tipo}. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tipo")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MensajeEntrante.CrearSala.class, name = "crearSala"),
        @JsonSubTypes.Type(value = MensajeEntrante.Unirse.class, name = "unirse"),
        @JsonSubTypes.Type(value = MensajeEntrante.ConfigurarLimite.class, name = "configurarLimite"),
        @JsonSubTypes.Type(value = MensajeEntrante.Empezar.class, name = "empezar"),
        @JsonSubTypes.Type(value = MensajeEntrante.Ordenes.class, name = "ordenes"),
        @JsonSubTypes.Type(value = MensajeEntrante.Confirmar.class, name = "confirmar")
})
public sealed interface MensajeEntrante {

    record CrearSala(String apodo) implements MensajeEntrante {
    }

    record Unirse(String codigo, String apodo) implements MensajeEntrante {
    }

    record ConfigurarLimite(Integer limiteRondas) implements MensajeEntrante {
    }

    record Empezar() implements MensajeEntrante {
    }

    record Ordenes(List<OrdenDto> ordenes) implements MensajeEntrante {
    }

    record Confirmar() implements MensajeEntrante {
    }
}
