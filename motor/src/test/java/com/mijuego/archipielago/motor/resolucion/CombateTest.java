package com.mijuego.archipielago.motor.resolucion;

import static com.mijuego.archipielago.motor.mapa.IslaId.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mijuego.archipielago.motor.EstadoDePrueba;
import com.mijuego.archipielago.motor.modelo.EstadoIsla;
import com.mijuego.archipielago.motor.modelo.EstadoPartida;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Los barcos que deja el combate se comprueban en el informe; el estado final incluye además la
 * producción (+1 por isla de jugador activo).
 */
class CombateTest {

    /** Tres jugadores con islas de salida para que nadie quede eliminado por accidente. */
    private static EstadoDePrueba tresJugadores() {
        return EstadoDePrueba.con("Ana", "Beto", "Cris")
                .isla(S1, "Ana", 1).isla(S2, "Beto", 1).isla(S3, "Cris", 1);
    }

    private static Map<String, Integer> fuerzas(Object... paresApodoFuerza) {
        Map<String, Integer> fuerzas = new LinkedHashMap<>();
        for (int i = 0; i < paresApodoFuerza.length; i += 2) {
            fuerzas.put((String) paresApodoFuerza[i], (Integer) paresApodoFuerza[i + 1]);
        }
        return fuerzas;
    }

    @Nested
    @DisplayName("Requirement: Combate en una isla")
    class CombateEnUnaIsla {

        @Test
        @DisplayName("Gana el mayor y pierde lo del segundo")
        void ganaElMayor() {
            EstadoPartida estado = tresJugadores().isla(I2, "Cris", 2)
                    .llega("Ana", 5, B12, I2).llega("Beto", 4, S2, I2).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().combates()).containsExactly(
                    new Combate(I2, fuerzas("Ana", 5, "Beto", 4, "Cris", 2), "Ana", 1));
            assertThat(r.estado().isla(I2)).isEqualTo(new EstadoIsla(I2, "Ana", 1 + 1));
            assertThat(r.estado().flotas()).isEmpty();
        }

        @Test
        @DisplayName("Empate con el defensor")
        void empateConElDefensor() {
            EstadoPartida estado = tresJugadores().isla(I1, "Ana", 3).llega("Beto", 3, B12, I1).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().combates()).containsExactly(
                    new Combate(I1, fuerzas("Ana", 3, "Beto", 3), "Ana", 0));
            assertThat(r.estado().isla(I1)).isEqualTo(new EstadoIsla(I1, "Ana", 0 + 1));
            assertThat(r.informe().capturas()).isEmpty();
        }

        @Test
        @DisplayName("Empate entre atacantes")
        void empateEntreAtacantes() {
            EstadoPartida estado = tresJugadores().isla(I2, "Cris", 2)
                    .llega("Ana", 5, B12, I2).llega("Beto", 5, S2, I2).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            Combate combate = r.informe().combates().getFirst();
            assertThat(combate.hayGanador()).isFalse();
            assertThat(combate.barcosRestantes()).isZero();
            assertThat(combate.fuerzas()).isEqualTo(fuerzas("Ana", 5, "Beto", 5, "Cris", 2));
            assertThat(r.estado().isla(I2)).isEqualTo(new EstadoIsla(I2, "Cris", 0 + 1));
            assertThat(r.informe().capturas()).isEmpty();
        }

        @Test
        @DisplayName("Empate en una isla neutral")
        void empateEnUnaIslaNeutral() {
            EstadoPartida estado = tresJugadores().llega("Ana", 4, I1, C).llega("Beto", 4, I2, C).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().combates()).containsExactly(
                    new Combate(C, fuerzas("Ana", 4, "Beto", 4), null, 0));
            assertThat(r.estado().isla(C)).isEqualTo(EstadoIsla.neutral(C));
            assertThat(r.estado().flotas()).isEmpty();
        }

        @Test
        @DisplayName("Refuerzo del defensor")
        void refuerzoDelDefensor() {
            EstadoPartida estado = tresJugadores().isla(I1, "Ana", 2)
                    .llega("Ana", 3, S1, I1).llega("Beto", 4, B12, I1).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().combates()).containsExactly(
                    new Combate(I1, fuerzas("Ana", 5, "Beto", 4), "Ana", 1));
            assertThat(r.estado().isla(I1)).isEqualTo(new EstadoIsla(I1, "Ana", 1 + 1));
        }

        @Test
        @DisplayName("Refuerzo sin enemigos")
        void refuerzoSinEnemigos() {
            EstadoPartida estado = tresJugadores().isla(I1, "Ana", 2).llega("Ana", 3, S1, I1).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().combates()).isEmpty();
            assertThat(r.informe().capturas()).isEmpty();
            assertThat(r.estado().isla(I1)).isEqualTo(new EstadoIsla(I1, "Ana", 5 + 1));
        }

        @Test
        @DisplayName("Un defensor con 0 barcos no cuenta como fuerza")
        void defensorSinBarcos() {
            EstadoPartida estado = tresJugadores().isla(I2, "Cris", 0)
                    .llega("Ana", 3, B12, I2).llega("Beto", 3, S2, I2).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().combates()).containsExactly(
                    new Combate(I2, fuerzas("Ana", 3, "Beto", 3), null, 0));
            assertThat(r.estado().isla(I2).dueno()).isEqualTo("Cris");
        }
    }

    @Nested
    @DisplayName("Requirement: Captura de islas")
    class Captura_ {

        @Test
        @DisplayName("Captura de una isla neutral")
        void capturaNeutral() {
            EstadoPartida estado = tresJugadores().llega("Ana", 3, I1, C).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().combates()).isEmpty();
            assertThat(r.informe().capturas()).containsExactly(new Captura(C, null, "Ana"));
            assertThat(r.estado().isla(C)).isEqualTo(new EstadoIsla(C, "Ana", 3 + 1));
        }

        @Test
        @DisplayName("Captura de una isla enemiga vacía")
        void capturaEnemigaVacia() {
            EstadoPartida estado = tresJugadores().isla(B23, "Beto", 0).llega("Ana", 2, I2, B23).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().combates()).isEmpty();
            assertThat(r.informe().capturas()).containsExactly(new Captura(B23, "Beto", "Ana"));
            assertThat(r.estado().isla(B23)).isEqualTo(new EstadoIsla(B23, "Ana", 2 + 1));
        }

        @Test
        @DisplayName("Captura tras ganar un combate a otro dueño")
        void capturaTrasCombate() {
            EstadoPartida estado = tresJugadores().isla(I2, "Cris", 2).llega("Ana", 5, B12, I2).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().capturas()).containsExactly(new Captura(I2, "Cris", "Ana"));
            assertThat(r.estado().isla(I2)).isEqualTo(new EstadoIsla(I2, "Ana", 3 + 1));
        }
    }

    @Nested
    @DisplayName("Requirement: Producción")
    class Produccion {

        @Test
        @DisplayName("Producción tras capturar")
        void produccionTrasCapturar() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto")
                    .isla(S1, "Ana", 2).isla(S3, "Beto", 5).llega("Ana", 3, I1, C).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().capturas()).containsExactly(new Captura(C, null, "Ana"));
            assertThat(r.estado().isla(S1)).isEqualTo(new EstadoIsla(S1, "Ana", 3));
            assertThat(r.estado().isla(C)).isEqualTo(new EstadoIsla(C, "Ana", 4));
        }

        @Test
        @DisplayName("Neutrales sin producción")
        void neutralesSinProduccion() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto").isla(S1, "Ana", 2).isla(S3, "Beto", 2).build();

            EstadoPartida siguiente = ResolutorRonda.resolver(estado, Map.of()).estado();

            assertThat(siguiente.isla(B34)).isEqualTo(EstadoIsla.neutral(B34));
            assertThat(siguiente.islas().values()).filteredOn(EstadoIsla::esNeutral)
                    .allSatisfy(i -> assertThat(i.barcos()).isZero());
        }

        @Test
        @DisplayName("Las islas con 0 barcos también producen")
        void islaVaciaProduce() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto").isla(S1, "Ana", 0).isla(S3, "Beto", 0).build();

            EstadoPartida siguiente = ResolutorRonda.resolver(estado, Map.of()).estado();

            assertThat(siguiente.isla(S1).barcos()).isEqualTo(1);
            assertThat(siguiente.isla(S3).barcos()).isEqualTo(1);
        }
    }
}
