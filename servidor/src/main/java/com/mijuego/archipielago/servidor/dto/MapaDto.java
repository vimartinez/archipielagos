package com.mijuego.archipielago.servidor.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.mijuego.archipielago.motor.mapa.IslaId;
import com.mijuego.archipielago.motor.mapa.Mapa;
import java.util.List;

/** Islas y rutas del mapa fijo, para que el cliente no duplique la tabla de rutas. */
@JsonTypeName("mapa")
public record MapaDto(List<String> islas, List<RutaDto> rutas) implements MensajeSaliente {

    public record RutaDto(String a, String b, int distancia) {
    }

    public static MapaDto delMapa() {
        return new MapaDto(
                Mapa.islas().stream().map(IslaId::name).toList(),
                Mapa.rutas().stream().map(r -> new RutaDto(r.a().name(), r.b().name(), r.distancia())).toList());
    }
}
