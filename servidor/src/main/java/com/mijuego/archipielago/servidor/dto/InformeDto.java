package com.mijuego.archipielago.servidor.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.mijuego.archipielago.motor.resolucion.InformeRonda;
import java.util.List;

/** Informe de una ronda resuelta: combates, capturas, eliminados y, si terminó la partida, ganadores. */
@JsonTypeName("informe")
public record InformeDto(
        int ronda,
        List<CombateDto> combates,
        List<CapturaDto> capturas,
        List<String> eliminados,
        List<String> ganadores) implements MensajeSaliente {

    /** Un combate; {@code ganador} es null si no hubo ganador único. */
    public record CombateDto(String isla, List<FuerzaDto> fuerzas, String ganador, int barcosRestantes) {
    }

    public record FuerzaDto(String apodo, int barcos) {
    }

    /** Cambio de dueño de una isla; {@code duenoAnterior} es null si era neutral. */
    public record CapturaDto(String isla, String duenoAnterior, String nuevoDueno) {
    }

    public static InformeDto de(InformeRonda informe) {
        return new InformeDto(
                informe.ronda(),
                informe.combates().stream()
                        .map(c -> new CombateDto(c.isla().name(),
                                c.fuerzas().entrySet().stream().map(e -> new FuerzaDto(e.getKey(), e.getValue())).toList(),
                                c.ganador(), c.barcosRestantes()))
                        .toList(),
                informe.capturas().stream()
                        .map(c -> new CapturaDto(c.isla().name(), c.duenoAnterior(), c.nuevoDueno()))
                        .toList(),
                informe.eliminados(),
                informe.ganadores());
    }
}
