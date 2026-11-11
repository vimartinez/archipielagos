package com.mijuego.archipielago.motor;

import static com.mijuego.archipielago.motor.mapa.IslaId.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mijuego.archipielago.motor.mapa.IslaId;
import com.mijuego.archipielago.motor.modelo.EstadoIsla;
import com.mijuego.archipielago.motor.modelo.EstadoPartida;
import com.mijuego.archipielago.motor.modelo.Flota;
import com.mijuego.archipielago.motor.modelo.Jugador;
import com.mijuego.archipielago.motor.modelo.Orden;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PartidaCreacionTest {

    @Nested
    @DisplayName("Requirement: Creación de partida")
    class Creacion {

        @Test
        @DisplayName("Partida válida con límite por defecto")
        void limitePorDefecto() {
            Partida partida = Partida.crear(List.of("Ana", "Beto"), null);
            assertThat(partida.estado().limiteRondas()).isEqualTo(25);
            assertThat(Partida.crear(List.of("Ana", "Beto")).estado().limiteRondas()).isEqualTo(25);
        }

        @Test
        @DisplayName("Límite de rondas configurado")
        void limiteConfigurado() {
            Partida partida = Partida.crear(List.of("Ana", "Beto", "Cris"), 40);
            assertThat(partida.estado().limiteRondas()).isEqualTo(40);
        }

        @ParameterizedTest
        @ValueSource(ints = {10, 50})
        @DisplayName("Los extremos del límite son válidos")
        void extremosValidos(int limite) {
            assertThat(Partida.crear(List.of("Ana", "Beto"), limite).estado().limiteRondas()).isEqualTo(limite);
        }

        @Test
        @DisplayName("Demasiados pocos jugadores")
        void unSoloJugador() {
            assertThatThrownBy(() -> Partida.crear(List.of("Ana"), null))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("entre 2 y 4 jugadores");
        }

        @Test
        @DisplayName("Demasiados jugadores")
        void cincoJugadores() {
            assertThatThrownBy(() -> Partida.crear(List.of("Ana", "Beto", "Cris", "Dani", "Eva"), null))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("entre 2 y 4 jugadores");
        }

        @ParameterizedTest
        @ValueSource(ints = {9, 51})
        @DisplayName("Límite de rondas fuera de rango")
        void limiteFueraDeRango(int limite) {
            assertThatThrownBy(() -> Partida.crear(List.of("Ana", "Beto"), limite))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("entre 10 y 50");
        }

        @Test
        @DisplayName("Apodos repetidos")
        void apodosRepetidos() {
            assertThatThrownBy(() -> Partida.crear(List.of("Ana", "Beto", "Ana"), null))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("'Ana'")
                    .hasMessageContaining("repetido");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Apodos vacíos")
        void apodoVacio(String apodo) {
            assertThatThrownBy(() -> Partida.crear(List.of("Ana", apodo), null))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("jugador 2")
                    .hasMessageContaining("vacío");
        }

        @Test
        @DisplayName("Apodo nulo")
        void apodoNulo() {
            assertThatThrownBy(() -> Partida.crear(Arrays.asList("Ana", null), null))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("jugador 2");
        }
    }

    @Nested
    @DisplayName("Requirement: Estado inicial")
    class EstadoInicial {

        @Test
        @DisplayName("Estado al empezar")
        void estadoAlEmpezar() {
            EstadoPartida estado = Partida.crear(List.of("Ana", "Beto")).estado();

            assertThat(estado.ronda()).isEqualTo(1);
            assertThat(estado.enCurso()).isTrue();
            assertThat(estado.isla(S1)).isEqualTo(new EstadoIsla(S1, "Ana", 5));
            assertThat(estado.isla(S3)).isEqualTo(new EstadoIsla(S3, "Beto", 5));
            assertThat(estado.islas().values())
                    .filteredOn(i -> i.isla() != S1 && i.isla() != S3)
                    .hasSize(11)
                    .allSatisfy(i -> {
                        assertThat(i.esNeutral()).isTrue();
                        assertThat(i.barcos()).isZero();
                    });
            assertThat(estado.flotas()).isEmpty();
            assertThat(estado.ganadores()).isEmpty();
        }

        @Test
        @DisplayName("Partida de 3 jugadores: salidas S1, S2 y S3, y S4 neutral vacía")
        void tresJugadores() {
            EstadoPartida estado = Partida.crear(List.of("Ana", "Beto", "Cris")).estado();
            assertThat(estado.isla(S1).dueno()).isEqualTo("Ana");
            assertThat(estado.isla(S2).dueno()).isEqualTo("Beto");
            assertThat(estado.isla(S3).dueno()).isEqualTo("Cris");
            assertThat(estado.isla(S4)).isEqualTo(EstadoIsla.neutral(S4));
        }

        @Test
        @DisplayName("Partida de 2 jugadores: S2 y S4 neutrales vacías")
        void dosJugadoresSalidasLibres() {
            EstadoPartida estado = Partida.crear(List.of("Ana", "Beto")).estado();
            assertThat(estado.isla(S2)).isEqualTo(EstadoIsla.neutral(S2));
            assertThat(estado.isla(S4)).isEqualTo(EstadoIsla.neutral(S4));
        }
    }

    @Nested
    @DisplayName("Requirement: Estado público de la partida")
    class EstadoPublico {

        @Test
        @DisplayName("Flotas enemigas visibles")
        void flotasEnemigasVisibles() {
            Partida partida = Partida.crear(List.of("Ana", "Beto"));
            partida.enviarOrdenes("Beto", List.of(new Orden(S3, B34, 3)));
            partida.resolverRonda();

            assertThat(partida.estado().flotas()).containsExactly(new Flota("Beto", 3, S3, B34, 2));
        }

        @Test
        @DisplayName("El estado indica ronda, límite, jugadores activos y eliminados")
        void jugadoresActivosYEliminados() {
            Partida partida = Partida.crear(List.of("Ana", "Beto", "Cris"), 30);
            partida.eliminarPorDesconexion("Beto");

            EstadoPartida estado = partida.estado();
            assertThat(estado.ronda()).isEqualTo(1);
            assertThat(estado.limiteRondas()).isEqualTo(30);
            assertThat(estado.enCurso()).isTrue();
            assertThat(estado.jugadoresActivos()).extracting(Jugador::apodo).containsExactly("Ana", "Cris");
            assertThat(estado.jugadoresEliminados()).extracting(Jugador::apodo).containsExactly("Beto");
            assertThat(estado.islas().keySet()).containsExactlyElementsOf(Set.of(IslaId.values()).stream()
                    .sorted().toList());
        }

        @Test
        @DisplayName("El estado es inmutable")
        void estadoInmutable() {
            EstadoPartida estado = Partida.crear(List.of("Ana", "Beto")).estado();
            assertThatThrownBy(() -> estado.islas().put(C, EstadoIsla.neutral(C)))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> estado.flotas().add(new Flota("Ana", 1, S1, I1, 1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
