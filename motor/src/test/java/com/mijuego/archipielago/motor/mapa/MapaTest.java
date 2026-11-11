package com.mijuego.archipielago.motor.mapa;

import static com.mijuego.archipielago.motor.mapa.IslaId.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MapaTest {

    @Nested
    @DisplayName("Requirement: Islas del mapa")
    class Islas {

        @Test
        @DisplayName("Consultar las islas")
        void contieneExactamenteLas13Islas() {
            assertThat(Mapa.islas()).containsExactlyInAnyOrder(
                    S1, S2, S3, S4, B12, B23, B34, B41, I1, I2, I3, I4, C);
        }
    }

    @Nested
    @DisplayName("Requirement: Rutas y distancias")
    class Rutas {

        @Test
        @DisplayName("Ruta corta hacia la interior")
        void rutaCortaHaciaLaInterior() {
            assertThat(Mapa.distancia(S1, I1)).hasValue(1);
        }

        @Test
        @DisplayName("Ruta larga directa a una bahía")
        void rutaLargaDirectaAUnaBahia() {
            assertThat(Mapa.distancia(S2, B12)).hasValue(3);
        }

        @Test
        @DisplayName("Ruta hacia el centro")
        void rutaHaciaElCentro() {
            assertThat(Mapa.distancia(I3, C)).hasValue(2);
        }

        @Test
        @DisplayName("Ruta en sentido contrario")
        void rutaEnSentidoContrario() {
            assertThat(Mapa.ruta(B41, I4)).isPresent();
            assertThat(Mapa.distancia(B41, I4)).isEqualTo(Mapa.distancia(I4, B41));
        }

        @Test
        @DisplayName("Islas no conectadas")
        void islasNoConectadas() {
            assertThat(Mapa.ruta(S1, C)).isEmpty();
            assertThat(Mapa.hayRuta(S1, C)).isFalse();
            assertThat(Mapa.distancia(S1, C)).isEmpty();
        }

        @Test
        @DisplayName("Recuento de rutas")
        void hayExactamente24Rutas() {
            assertThat(Mapa.rutas()).hasSize(24).doesNotHaveDuplicates();
        }

        @ParameterizedTest(name = "{0}–{1}: {2}")
        @CsvSource({
                "S1,I1,1", "S2,I2,1", "S3,I3,1", "S4,I4,1",
                "S1,B12,3", "S1,B41,3", "S2,B12,3", "S2,B23,3",
                "S3,B23,3", "S3,B34,3", "S4,B34,3", "S4,B41,3",
                "I1,B12,1", "I1,B41,1", "I2,B12,1", "I2,B23,1",
                "I3,B23,1", "I3,B34,1", "I4,B34,1", "I4,B41,1",
                "I1,C,2", "I2,C,2", "I3,C,2", "I4,C,2"
        })
        @DisplayName("Las 24 rutas de la spec existen en ambos sentidos con su distancia")
        void rutasDeLaSpec(IslaId a, IslaId b, int distancia) {
            assertThat(Mapa.distancia(a, b)).hasValue(distancia);
            assertThat(Mapa.distancia(b, a)).hasValue(distancia);
        }

        @Test
        @DisplayName("No existen rutas entre islas no listadas")
        void noHayOtrasRutas() {
            long pares = Mapa.islas().stream()
                    .flatMap(a -> Mapa.islas().stream().filter(b -> a.ordinal() < b.ordinal() && Mapa.hayRuta(a, b)))
                    .count();
            assertThat(pares).isEqualTo(24);
            assertThat(Mapa.hayRuta(S1, S2)).isFalse();
            assertThat(Mapa.hayRuta(S1, I2)).isFalse();
            assertThat(Mapa.hayRuta(B12, C)).isFalse();
            assertThat(Mapa.hayRuta(S1, B23)).isFalse();
        }
    }

    @Nested
    @DisplayName("Requirement: Simetría del mapa")
    class Simetria {

        @Test
        @DisplayName("Rotar el mapa")
        void rotarElMapaDaElMismoConjunto() {
            Set<Ruta> originales = Set.copyOf(Mapa.rutas());
            Set<Ruta> rotadas = Mapa.rutas().stream()
                    .map(r -> new Ruta(rotar(r.a()), rotar(r.b()), r.distancia()))
                    .collect(Collectors.toSet());
            assertThat(rotadas).isEqualTo(originales);
        }

        private IslaId rotar(IslaId isla) {
            String nombre = isla.name();
            StringBuilder rotado = new StringBuilder();
            for (char c : nombre.toCharArray()) {
                rotado.append(Character.isDigit(c) ? (char) ('0' + IslaId.siguiente(c - '0')) : c);
            }
            return IslaId.valueOf(rotado.toString());
        }
    }

    @Nested
    @DisplayName("Requirement: Islas de salida según el número de jugadores")
    class Salidas {

        @Test
        @DisplayName("Partida de 2 jugadores")
        void dosJugadores() {
            assertThat(Mapa.salidas(2)).containsExactly(S1, S3);
        }

        @Test
        @DisplayName("Partida de 3 jugadores")
        void tresJugadores() {
            assertThat(Mapa.salidas(3)).containsExactly(S1, S2, S3);
        }

        @Test
        @DisplayName("Partida de 4 jugadores")
        void cuatroJugadores() {
            assertThat(Mapa.salidas(4)).containsExactly(S1, S2, S3, S4);
        }

        @Test
        @DisplayName("Número de jugadores no soportado")
        void numeroNoSoportado() {
            assertThatThrownBy(() -> Mapa.salidas(5)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
