package com.mijuego.archipielago.motor;

import static com.mijuego.archipielago.motor.mapa.IslaId.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mijuego.archipielago.motor.mapa.IslaId;
import com.mijuego.archipielago.motor.modelo.EstadoIsla;
import com.mijuego.archipielago.motor.modelo.EstadoPartida;
import com.mijuego.archipielago.motor.modelo.Flota;
import com.mijuego.archipielago.motor.modelo.Orden;
import com.mijuego.archipielago.motor.resolucion.Captura;
import com.mijuego.archipielago.motor.resolucion.Combate;
import com.mijuego.archipielago.motor.resolucion.InformeRonda;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Partida completa de 3 jugadores usando únicamente la API pública de {@link Partida}. */
class PartidaCompletaTest {

    @Test
    @DisplayName("Partida de 3 jugadores: movimientos, combates, una eliminación y fin por límite de rondas")
    void partidaCompleta() {
        Partida partida = Partida.crear(List.of("Ana", "Beto", "Cris"), 10);

        // Ronda 1: cada uno ocupa su isla interior (distancia 1).
        partida.enviarOrdenes("Ana", List.of(new Orden(S1, I1, 5)));
        partida.enviarOrdenes("Beto", List.of(new Orden(S2, I2, 5)));
        partida.enviarOrdenes("Cris", List.of(new Orden(S3, I3, 5)));
        InformeRonda r1 = partida.resolverRonda();

        assertThat(r1.combates()).isEmpty();
        assertThat(r1.capturas()).containsExactly(
                new Captura(I1, null, "Ana"), new Captura(I2, null, "Beto"), new Captura(I3, null, "Cris"));
        assertIslas(partida.estado(), S1, "Ana", 1, I1, "Ana", 6, S2, "Beto", 1, I2, "Beto", 6,
                S3, "Cris", 1, I3, "Cris", 6);

        // Ronda 2: Ana y Cris lanzan todo hacia C (distancia 2); Beto toma la bahía B23.
        partida.enviarOrdenes("Ana", List.of(new Orden(I1, C, 6)));
        partida.enviarOrdenes("Cris", List.of(new Orden(I3, C, 6)));
        partida.enviarOrdenes("Beto", List.of(new Orden(I2, B23, 6)));
        InformeRonda r2 = partida.resolverRonda();

        assertThat(r2.capturas()).containsExactly(new Captura(B23, null, "Beto"));
        assertThat(partida.estado().flotas()).containsExactly(
                new Flota("Ana", 6, I1, C, 1), new Flota("Cris", 6, I3, C, 1));
        assertIslas(partida.estado(), B23, "Beto", 7, I3, "Cris", 1);

        // Ronda 3: empate en C (neutral) que destruye ambas flotas; Beto conquista I3.
        partida.enviarOrdenes("Beto", List.of(new Orden(B23, I3, 7)));
        InformeRonda r3 = partida.resolverRonda();

        assertThat(r3.combates()).containsExactly(
                new Combate(I3, fuerzas("Beto", 7, "Cris", 1), "Beto", 6),
                new Combate(C, fuerzas("Ana", 6, "Cris", 6), null, 0));
        assertThat(r3.capturas()).containsExactly(new Captura(I3, "Cris", "Beto"));
        assertThat(partida.estado().flotas()).isEmpty();
        assertIslas(partida.estado(), C, null, 0, I3, "Beto", 7, S3, "Cris", 3);

        // Ronda 4: Beto toma S3, la última isla de Cris, que queda eliminada.
        partida.enviarOrdenes("Beto", List.of(new Orden(I3, S3, 7)));
        InformeRonda r4 = partida.resolverRonda();

        assertThat(r4.combates()).containsExactly(new Combate(S3, fuerzas("Beto", 7, "Cris", 3), "Beto", 4));
        assertThat(r4.eliminados()).containsExactly("Cris");
        assertThat(r4.ganadores()).isEmpty();
        assertThat(partida.estado().enCurso()).isTrue();
        assertThat(partida.estado().jugadoresActivos()).extracting("apodo").containsExactly("Ana", "Beto");
        assertThatThrownBy(() -> partida.enviarOrdenes("Cris", List.of()))
                .isInstanceOf(ReglaInfringidaException.class);

        // Ronda 5: Ana manda una flota lenta (distancia 3) a B12, que llega en la ronda 7.
        partida.enviarOrdenes("Ana", List.of(new Orden(S1, B12, 3)));
        partida.resolverRonda();
        assertThat(partida.estado().flotas()).containsExactly(new Flota("Ana", 3, S1, B12, 2));
        partida.resolverRonda();
        assertThat(partida.estado().flotas()).containsExactly(new Flota("Ana", 3, S1, B12, 1));
        InformeRonda r7 = partida.resolverRonda();
        assertThat(r7.capturas()).containsExactly(new Captura(B12, null, "Ana"));

        // Rondas 8 a 10: nadie se mueve; al resolver la ronda 10 se alcanza el límite.
        partida.resolverRonda();
        partida.resolverRonda();
        assertThat(partida.estado().ronda()).isEqualTo(10);
        InformeRonda r10 = partida.resolverRonda();

        EstadoPartida fin = partida.estado();
        assertThat(r10.ronda()).isEqualTo(10);
        assertThat(fin.enCurso()).isFalse();
        assertThat(fin.ronda()).isEqualTo(10);
        assertThat(fin.islasDe("Ana")).extracting(EstadoIsla::isla).containsExactly(S1, B12, I1);
        assertThat(fin.islasDe("Beto")).extracting(EstadoIsla::isla).containsExactly(S2, S3, B23, I2, I3);
        assertThat(r10.ganadores()).containsExactly("Beto");
        assertThat(fin.ganadores()).containsExactly("Beto");
        assertIslas(fin, S1, "Ana", 7, B12, "Ana", 7, I1, "Ana", 9,
                S2, "Beto", 10, S3, "Beto", 11, B23, "Beto", 8, I2, "Beto", 9, I3, "Beto", 7);
        assertThatThrownBy(partida::resolverRonda)
                .isInstanceOf(ReglaInfringidaException.class)
                .hasMessageContaining("La partida ha terminado");
    }

    private static Map<String, Integer> fuerzas(Object... pares) {
        Map<String, Integer> fuerzas = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            fuerzas.put((String) pares[i], (Integer) pares[i + 1]);
        }
        return fuerzas;
    }

    /** Comprueba tríos (isla, dueño, barcos). */
    private static void assertIslas(EstadoPartida estado, Object... trios) {
        for (int i = 0; i < trios.length; i += 3) {
            IslaId isla = (IslaId) trios[i];
            assertThat(estado.isla(isla)).as("isla %s", isla)
                    .isEqualTo(new EstadoIsla(isla, (String) trios[i + 1], (Integer) trios[i + 2]));
        }
    }
}
