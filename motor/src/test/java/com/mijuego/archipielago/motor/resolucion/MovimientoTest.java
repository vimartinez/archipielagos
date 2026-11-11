package com.mijuego.archipielago.motor.resolucion;

import static com.mijuego.archipielago.motor.mapa.IslaId.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mijuego.archipielago.motor.EstadoDePrueba;
import com.mijuego.archipielago.motor.modelo.EstadoIsla;
import com.mijuego.archipielago.motor.modelo.EstadoPartida;
import com.mijuego.archipielago.motor.modelo.Flota;
import com.mijuego.archipielago.motor.modelo.Orden;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MovimientoTest {

    @Nested
    @DisplayName("Requirement: Salida, avance y llegada de flotas")
    class SalidaAvanceLlegada {

        @Test
        @DisplayName("Ruta de distancia 1")
        void rutaDeDistancia1() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto").ronda(2)
                    .isla(S1, "Ana", 6).isla(S3, "Beto", 6).build();

            ResultadoRonda resultado = ResolutorRonda.resolver(estado,
                    Map.of("Ana", List.of(new Orden(S1, I1, 4))));

            assertThat(resultado.informe().ronda()).isEqualTo(2);
            assertThat(resultado.estado().flotas()).isEmpty();
            assertThat(resultado.estado().isla(I1)).isEqualTo(new EstadoIsla(I1, "Ana", 4 + 1));
            assertThat(resultado.estado().isla(S1)).isEqualTo(new EstadoIsla(S1, "Ana", 2 + 1));
            assertThat(resultado.informe().capturas()).containsExactly(new Captura(I1, null, "Ana"));
        }

        @Test
        @DisplayName("Ruta de distancia 3")
        void rutaDeDistancia3() {
            EstadoPartida ronda3 = EstadoDePrueba.con("Ana", "Beto").ronda(3)
                    .isla(S1, "Ana", 6).isla(S3, "Beto", 6).build();

            EstadoPartida ronda4 = ResolutorRonda.resolver(ronda3,
                    Map.of("Ana", List.of(new Orden(S1, B12, 4)))).estado();
            assertThat(ronda4.ronda()).isEqualTo(4);
            assertThat(ronda4.flotas()).containsExactly(new Flota("Ana", 4, S1, B12, 2));

            EstadoPartida ronda5 = ResolutorRonda.resolver(ronda4, Map.of()).estado();
            assertThat(ronda5.flotas()).containsExactly(new Flota("Ana", 4, S1, B12, 1));
            assertThat(ronda5.isla(B12).esNeutral()).isTrue();

            ResultadoRonda resolucion5 = ResolutorRonda.resolver(ronda5, Map.of());
            assertThat(resolucion5.informe().ronda()).isEqualTo(5);
            assertThat(resolucion5.estado().flotas()).isEmpty();
            assertThat(resolucion5.estado().isla(B12)).isEqualTo(new EstadoIsla(B12, "Ana", 4 + 1));
        }

        @Test
        @DisplayName("Vaciar una isla: el origen sigue siendo del jugador")
        void vaciarUnaIsla() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto")
                    .isla(S1, "Ana", 5).isla(S3, "Beto", 5).build();

            EstadoPartida siguiente = ResolutorRonda.resolver(estado,
                    Map.of("Ana", List.of(new Orden(S1, I1, 5)))).estado();

            assertThat(siguiente.isla(S1)).isEqualTo(new EstadoIsla(S1, "Ana", 0 + 1));
            assertThat(siguiente.isla(I1)).isEqualTo(new EstadoIsla(I1, "Ana", 5 + 1));
        }

        @Test
        @DisplayName("Varias órdenes desde el mismo origen crean flotas independientes")
        void variasOrdenes() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto")
                    .isla(S1, "Ana", 5).isla(S3, "Beto", 5).build();

            EstadoPartida siguiente = ResolutorRonda.resolver(estado,
                    Map.of("Ana", List.of(new Orden(S1, B12, 2), new Orden(S1, B41, 3)))).estado();

            assertThat(siguiente.flotas()).containsExactly(
                    new Flota("Ana", 2, S1, B12, 2), new Flota("Ana", 3, S1, B41, 2));
            assertThat(siguiente.isla(S1).barcos()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Requirement: Flotas que se cruzan")
    class Cruce {

        @Test
        @DisplayName("Cruce en la misma ruta")
        void cruceEnLaMismaRuta() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto")
                    .isla(S1, "Ana", 1).isla(B12, "Beto", 0).isla(S3, "Beto", 1)
                    .flota("Ana", 4, S1, B12, 2)
                    .flota("Beto", 2, B12, S1, 2)
                    .build();

            ResultadoRonda primera = ResolutorRonda.resolver(estado, Map.of());
            assertThat(primera.estado().flotas()).containsExactly(
                    new Flota("Ana", 4, S1, B12, 1), new Flota("Beto", 2, B12, S1, 1));
            assertThat(primera.informe().combates()).isEmpty();

            ResultadoRonda segunda = ResolutorRonda.resolver(primera.estado(), Map.of());
            assertThat(segunda.estado().flotas()).isEmpty();
            assertThat(segunda.informe().combates())
                    .extracting(Combate::isla, Combate::fuerzas)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(S1, Map.of("Ana", 2, "Beto", 2)),
                            org.assertj.core.groups.Tuple.tuple(B12, Map.of("Ana", 4, "Beto", 1)));
        }
    }
}
