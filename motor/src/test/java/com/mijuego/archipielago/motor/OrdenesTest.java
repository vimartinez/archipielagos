package com.mijuego.archipielago.motor;

import static com.mijuego.archipielago.motor.mapa.IslaId.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mijuego.archipielago.motor.modelo.EstadoIsla;
import com.mijuego.archipielago.motor.modelo.Flota;
import com.mijuego.archipielago.motor.modelo.Orden;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OrdenesTest {

    private Partida partida;

    @BeforeEach
    void crearPartida() {
        partida = Partida.crear(List.of("Ana", "Beto"));
    }

    @Nested
    @DisplayName("Requirement: Envío de órdenes")
    class Envio {

        @Test
        @DisplayName("Sustituir las órdenes")
        void sustituirLasOrdenes() {
            partida.enviarOrdenes("Ana", List.of(new Orden(S1, I1, 3)));
            partida.enviarOrdenes("Ana", List.of(new Orden(S1, B12, 2)));
            partida.resolverRonda();

            assertThat(partida.estado().flotasDe("Ana")).containsExactly(new Flota("Ana", 2, S1, B12, 2));
            assertThat(partida.estado().isla(I1)).isEqualTo(EstadoIsla.neutral(I1));
            assertThat(partida.estado().isla(S1).barcos()).isEqualTo(5 - 2 + 1);
        }

        @Test
        @DisplayName("Jugador sin órdenes")
        void jugadorSinOrdenes() {
            partida.enviarOrdenes("Ana", List.of(new Orden(S1, I1, 2)));
            partida.resolverRonda();

            assertThat(partida.estado().flotasDe("Beto")).isEmpty();
            assertThat(partida.estado().isla(S3)).isEqualTo(new EstadoIsla(S3, "Beto", 6));
        }

        @Test
        @DisplayName("Un conjunto vacío es válido y no mueve nada")
        void conjuntoVacio() {
            partida.enviarOrdenes("Ana", List.of(new Orden(S1, I1, 2)));
            partida.enviarOrdenes("Ana", List.of());
            partida.resolverRonda();

            assertThat(partida.estado().flotas()).isEmpty();
            assertThat(partida.estado().isla(S1).barcos()).isEqualTo(6);
            assertThat(partida.estado().isla(I1).esNeutral()).isTrue();
        }
    }

    @Nested
    @DisplayName("Requirement: Validación de órdenes")
    class Validacion {

        @Test
        @DisplayName("Origen ajeno")
        void origenAjeno() {
            assertThatThrownBy(() -> partida.enviarOrdenes("Ana", List.of(new Orden(S3, I3, 1))))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("S3 no es de Ana");
        }

        @Test
        @DisplayName("Origen neutral")
        void origenNeutral() {
            assertThatThrownBy(() -> partida.enviarOrdenes("Ana", List.of(new Orden(I1, C, 1))))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("I1 no es de Ana");
        }

        @Test
        @DisplayName("Sin ruta directa")
        void sinRutaDirecta() {
            assertThatThrownBy(() -> partida.enviarOrdenes("Ana", List.of(new Orden(S1, C, 1))))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("no hay ruta entre S1 y C");
        }

        @Test
        @DisplayName("Cero barcos")
        void ceroBarcos() {
            assertThatThrownBy(() -> partida.enviarOrdenes("Ana", List.of(new Orden(S1, I1, 0))))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("al menos 1 barco");
        }

        @Test
        @DisplayName("Barcos negativos")
        void barcosNegativos() {
            assertThatThrownBy(() -> partida.enviarOrdenes("Ana", List.of(new Orden(S1, I1, -2))))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("al menos 1 barco");
        }

        @Test
        @DisplayName("Más barcos de los disponibles")
        void masBarcosDeLosDisponibles() {
            assertThatThrownBy(() -> partida.enviarOrdenes("Ana",
                    List.of(new Orden(S1, I1, 3), new Orden(S1, B12, 3))))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("S1 solo tiene 5 barcos")
                    .hasMessageContaining("Orden 2 (3 de S1 a B12)");
        }

        @Test
        @DisplayName("Vaciar una isla")
        void vaciarUnaIsla() {
            partida.enviarOrdenes("Ana", List.of(new Orden(S1, B12, 5)));
            partida.resolverRonda();

            // Tras la salida S1 queda con 0 barcos; la producción de la ronda le suma 1.
            assertThat(partida.estado().isla(S1)).isEqualTo(new EstadoIsla(S1, "Ana", 1));
            assertThat(partida.estado().flotasDe("Ana")).containsExactly(new Flota("Ana", 5, S1, B12, 2));
        }

        @Test
        @DisplayName("Un conjunto inválido se rechaza completo y conserva las órdenes previas")
        void rechazoTodoONada() {
            List<Orden> validas = List.of(new Orden(S1, I1, 2));
            partida.enviarOrdenes("Ana", validas);

            assertThatThrownBy(() -> partida.enviarOrdenes("Ana",
                    List.of(new Orden(S1, B12, 1), new Orden(S1, C, 1))))
                    .isInstanceOf(ReglaInfringidaException.class);

            assertThat(partida.ordenesPendientes()).containsEntry("Ana", validas);
        }

        @Test
        @DisplayName("La validación usa los barcos al inicio de la ronda, no los de la producción")
        void validaContraElInicioDeLaRonda() {
            assertThatThrownBy(() -> partida.enviarOrdenes("Ana", List.of(new Orden(S1, I1, 6))))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("S1 solo tiene 5 barcos");
        }

        @Test
        @DisplayName("Jugador desconocido")
        void jugadorDesconocido() {
            assertThatThrownBy(() -> partida.enviarOrdenes("Zoe", List.of()))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("'Zoe' no está en la partida");
        }
    }

    @Nested
    @DisplayName("Rechazo por estado del jugador o de la partida")
    class Rechazos {

        @Test
        @DisplayName("Órdenes de un jugador eliminado")
        void jugadorEliminado() {
            Partida partida3 = Partida.crear(List.of("Ana", "Beto", "Cris"));
            partida3.eliminarPorDesconexion("Cris");

            assertThatThrownBy(() -> partida3.enviarOrdenes("Cris", List.of()))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("'Cris' está eliminado");
        }

        @Test
        @DisplayName("Órdenes en una partida terminada")
        void partidaTerminada() {
            partida.eliminarPorDesconexion("Beto");

            assertThatThrownBy(() -> partida.enviarOrdenes("Ana", List.of()))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("La partida ha terminado");
        }
    }
}
