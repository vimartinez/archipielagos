package com.mijuego.archipielago.servidor.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mijuego.archipielago.motor.Partida;
import com.mijuego.archipielago.motor.mapa.IslaId;
import com.mijuego.archipielago.motor.modelo.Orden;
import com.mijuego.archipielago.motor.resolucion.Captura;
import com.mijuego.archipielago.motor.resolucion.Combate;
import com.mijuego.archipielago.motor.resolucion.InformeRonda;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ProtocoloJsonTest {

    private final JsonMapper json = JsonMapper.builder().build();

    private JsonNode serializar(MensajeSaliente mensaje) {
        return json.readTree(json.writeValueAsString(mensaje));
    }

    @Test
    void estadoConFlotaEnTransito() {
        Partida partida = Partida.crear(List.of("Ana", "Beto", "Cris"));
        partida.enviarOrdenes("Beto", List.of(new Orden(IslaId.S2, IslaId.B23, 3)));
        partida.resolverRonda();

        JsonNode nodo = serializar(PartidaDto.de(partida.estado(), "Ana", 42_000, List.of("Cris")));

        assertThat(nodo.get("tipo").asString()).isEqualTo("partida");
        assertThat(nodo.get("tuApodo").asString()).isEqualTo("Ana");
        assertThat(nodo.get("ronda").asInt()).isEqualTo(2);
        assertThat(nodo.get("limiteRondas").asInt()).isEqualTo(25);
        assertThat(nodo.get("enCurso").asBoolean()).isTrue();
        assertThat(nodo.get("msRestantes").asLong()).isEqualTo(42_000);
        assertThat(nodo.get("confirmados").get(0).asString()).isEqualTo("Cris");
        assertThat(nodo.get("islas")).hasSize(13);
        JsonNode i1 = nodo.get("islas").get(8);
        assertThat(i1.get("id").asString()).isEqualTo("I1");
        assertThat(i1.get("dueno").isNull()).isTrue();
        JsonNode flota = nodo.get("flotas").get(0);
        assertThat(flota.get("dueno").asString()).isEqualTo("Beto");
        assertThat(flota.get("barcos").asInt()).isEqualTo(3);
        assertThat(flota.get("origen").asString()).isEqualTo("S2");
        assertThat(flota.get("destino").asString()).isEqualTo("B23");
        assertThat(flota.get("rondasRestantes").asInt()).isEqualTo(2);
        assertThat(flota.get("distancia").asInt()).isEqualTo(3);
        JsonNode beto = nodo.get("jugadores").get(1);
        assertThat(beto.get("apodo").asString()).isEqualTo("Beto");
        assertThat(beto.get("activo").asBoolean()).isTrue();
        assertThat(beto.get("islas").asInt()).isEqualTo(1);
        assertThat(beto.get("barcos").asInt()).isEqualTo(6); // 2 + 1 de producción en S2, más 3 en tránsito
    }

    @Test
    void informeConCombate() {
        Map<String, Integer> fuerzas = new LinkedHashMap<>();
        fuerzas.put("Ana", 5);
        fuerzas.put("Beto", 4);
        InformeRonda informe = new InformeRonda(7,
                List.of(new Combate(IslaId.I2, fuerzas, "Ana", 1)),
                List.of(new Captura(IslaId.I2, "Beto", "Ana")),
                List.of(), List.of());

        JsonNode nodo = serializar(InformeDto.de(informe));

        assertThat(nodo.get("tipo").asString()).isEqualTo("informe");
        assertThat(nodo.get("ronda").asInt()).isEqualTo(7);
        JsonNode combate = nodo.get("combates").get(0);
        assertThat(combate.get("isla").asString()).isEqualTo("I2");
        assertThat(combate.get("fuerzas").get(0).get("apodo").asString()).isEqualTo("Ana");
        assertThat(combate.get("fuerzas").get(1).get("barcos").asInt()).isEqualTo(4);
        assertThat(combate.get("ganador").asString()).isEqualTo("Ana");
        assertThat(combate.get("barcosRestantes").asInt()).isEqualTo(1);
        assertThat(nodo.get("capturas").get(0).get("nuevoDueno").asString()).isEqualTo("Ana");
        assertThat(nodo.get("eliminados")).isEmpty();
        assertThat(nodo.get("ganadores")).isEmpty();
    }

    @Test
    void mapaSalaYError() {
        JsonNode mapa = serializar(MapaDto.delMapa());
        assertThat(mapa.get("tipo").asString()).isEqualTo("mapa");
        assertThat(mapa.get("islas")).hasSize(13);
        assertThat(mapa.get("rutas")).hasSize(24);
        assertThat(mapa.get("rutas").get(0).get("distancia").asInt()).isEqualTo(1);

        JsonNode sala = serializar(new SalaDto("K7QX", "Beto", List.of("Ana", "Beto"), "Ana", 25));
        assertThat(sala.get("tipo").asString()).isEqualTo("sala");
        assertThat(sala.get("anfitrion").asString()).isEqualTo("Ana");

        JsonNode error = serializar(new ErrorDto("La sala no existe", "unirse"));
        assertThat(error.get("tipo").asString()).isEqualTo("error");
        assertThat(error.get("peticion").asString()).isEqualTo("unirse");
    }

    @Test
    void mensajesEntrantes() {
        assertThat(json.readValue("{\"tipo\":\"unirse\",\"codigo\":\"k7qx\",\"apodo\":\"Beto\"}", MensajeEntrante.class))
                .isEqualTo(new MensajeEntrante.Unirse("k7qx", "Beto"));
        assertThat(json.readValue("{\"tipo\":\"empezar\"}", MensajeEntrante.class))
                .isEqualTo(new MensajeEntrante.Empezar());
        assertThat(json.readValue(
                "{\"tipo\":\"ordenes\",\"ordenes\":[{\"origen\":\"S1\",\"destino\":\"I1\",\"barcos\":3}]}",
                MensajeEntrante.class))
                .isEqualTo(new MensajeEntrante.Ordenes(List.of(new OrdenDto("S1", "I1", 3))));
        assertThatThrownBy(() -> json.readValue("{\"tipo\":\"bailar\"}", MensajeEntrante.class))
                .isInstanceOf(JacksonException.class);
    }
}
