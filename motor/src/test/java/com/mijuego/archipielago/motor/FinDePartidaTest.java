package com.mijuego.archipielago.motor;

import static com.mijuego.archipielago.motor.mapa.IslaId.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mijuego.archipielago.motor.modelo.EstadoIsla;
import com.mijuego.archipielago.motor.modelo.EstadoPartida;
import com.mijuego.archipielago.motor.modelo.Jugador;
import com.mijuego.archipielago.motor.modelo.Orden;
import com.mijuego.archipielago.motor.resolucion.InformeRonda;
import com.mijuego.archipielago.motor.resolucion.ResolutorRonda;
import com.mijuego.archipielago.motor.resolucion.ResultadoRonda;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FinDePartidaTest {

    @Nested
    @DisplayName("Requirement: Eliminación por quedarse sin fuerzas")
    class EliminacionSinFuerzas {

        @Test
        @DisplayName("Jugador sin islas ni flotas")
        void sinIslasNiFlotas() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto", "Cris")
                    .isla(S1, "Ana", 3).isla(S2, "Cris", 3).isla(I3, "Beto", 1)
                    .llega("Ana", 5, B23, I3).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().eliminados()).containsExactly("Beto");
            assertThat(r.estado().jugador("Beto")).contains(new Jugador("Beto", false));
            assertThat(r.estado().enCurso()).isTrue();
        }

        @Test
        @DisplayName("Jugador sin islas pero con flotas")
        void sinIslasPeroConFlotas() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto")
                    .isla(S1, "Ana", 3).isla(I3, "Beto", 1)
                    .llega("Ana", 5, B23, I3)
                    .flota("Beto", 2, S3, B34, 2)
                    .build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.estado().islasDe("Beto")).isEmpty();
            assertThat(r.informe().eliminados()).isEmpty();
            assertThat(r.estado().jugador("Beto").orElseThrow().activo()).isTrue();
            assertThat(r.estado().enCurso()).isTrue();
        }
    }

    @Nested
    @DisplayName("Requirement: Eliminación por desconexión")
    class EliminacionPorDesconexion {

        @Test
        @DisplayName("Desconexión con islas y flotas")
        void desconexionConIslasYFlotas() {
            // Cris se une en segundo lugar para empezar en S2, como en el escenario de la spec.
            Partida partida = Partida.crear(List.of("Ana", "Cris", "Beto"));
            partida.enviarOrdenes("Cris", List.of(new Orden(S2, I2, 2), new Orden(S2, B23, 1)));
            partida.resolverRonda();
            assertThat(partida.estado().islasDe("Cris")).extracting(EstadoIsla::isla).containsExactly(S2, I2);
            assertThat(partida.estado().flotasDe("Cris")).hasSize(1);
            partida.enviarOrdenes("Cris", List.of(new Orden(S2, I2, 1)));

            partida.eliminarPorDesconexion("Cris");

            EstadoPartida estado = partida.estado();
            assertThat(estado.isla(S2)).isEqualTo(EstadoIsla.neutral(S2));
            assertThat(estado.isla(I2)).isEqualTo(EstadoIsla.neutral(I2));
            assertThat(estado.flotasDe("Cris")).isEmpty();
            assertThat(estado.jugador("Cris")).contains(new Jugador("Cris", false));
            assertThat(partida.ordenesPendientes()).doesNotContainKey("Cris");
            assertThat(estado.enCurso()).isTrue();
        }

        @Test
        @DisplayName("Desconexión que deja un solo jugador")
        void desconexionDejaUnSoloJugador() {
            Partida partida = Partida.crear(List.of("Ana", "Beto"));
            partida.eliminarPorDesconexion("Beto");

            assertThat(partida.estado().enCurso()).isFalse();
            assertThat(partida.estado().ganadores()).containsExactly("Ana");
        }

        @Test
        @DisplayName("Órdenes de un jugador eliminado")
        void ordenesDeUnEliminado() {
            Partida partida = Partida.crear(List.of("Ana", "Beto", "Cris"));
            partida.eliminarPorDesconexion("Beto");

            assertThatThrownBy(() -> partida.enviarOrdenes("Beto", List.of()))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("eliminado");
        }

        @Test
        @DisplayName("No se puede desconectar dos veces al mismo jugador")
        void desconectarEliminado() {
            Partida partida = Partida.crear(List.of("Ana", "Beto", "Cris"));
            partida.eliminarPorDesconexion("Beto");

            assertThatThrownBy(() -> partida.eliminarPorDesconexion("Beto"))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("eliminado");
        }

        @Test
        @DisplayName("Las islas de un desconectado no producen en la ronda siguiente")
        void islasNoProducen() {
            Partida partida = Partida.crear(List.of("Ana", "Beto", "Cris"));
            partida.eliminarPorDesconexion("Beto");
            partida.resolverRonda();

            assertThat(partida.estado().isla(S2)).isEqualTo(EstadoIsla.neutral(S2));
        }
    }

    @Nested
    @DisplayName("Requirement: Fin por último jugador en pie")
    class UltimoEnPie {

        @Test
        @DisplayName("Victoria anticipada")
        void victoriaAnticipada() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto").ronda(12).limite(25)
                    .isla(S1, "Ana", 3).isla(I3, "Beto", 1)
                    .llega("Ana", 5, B23, I3).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.estado().enCurso()).isFalse();
            assertThat(r.estado().ronda()).isEqualTo(12);
            assertThat(r.estado().ganadores()).containsExactly("Ana");
            assertThat(r.informe().ganadores()).containsExactly("Ana");
            assertThat(r.informe().eliminados()).containsExactly("Beto");
        }

        @Test
        @DisplayName("Eliminación simultánea de todos")
        void eliminacionSimultanea() {
            // Cris (ya eliminada) les quitó sus islas; Ana y Beto solo tienen flotas que empatan en C.
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto", "Cris").eliminado("Cris")
                    .llega("Ana", 4, I1, C).llega("Beto", 4, I3, C).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().eliminados()).containsExactly("Ana", "Beto");
            assertThat(r.estado().enCurso()).isFalse();
            assertThat(r.estado().ganadores()).containsExactly("Ana", "Beto");
        }
    }

    @Nested
    @DisplayName("Requirement: Fin por límite de rondas")
    class LimiteDeRondas {

        private EstadoDePrueba ultimaRonda() {
            return EstadoDePrueba.con("Ana", "Beto").ronda(25).limite(25);
        }

        @Test
        @DisplayName("Gana quien tiene más islas")
        void ganaQuienTieneMasIslas() {
            EstadoPartida estado = ultimaRonda()
                    .isla(S1, "Ana", 1).isla(I1, "Ana", 1).isla(B12, "Ana", 1)
                    .isla(B41, "Ana", 1).isla(C, "Ana", 1).isla(I2, "Ana", 1)
                    .isla(S3, "Beto", 20).isla(I3, "Beto", 20).isla(B23, "Beto", 20).isla(B34, "Beto", 20)
                    .build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.estado().enCurso()).isFalse();
            assertThat(r.estado().ganadores()).containsExactly("Ana");
            assertThat(r.estado().ronda()).isEqualTo(25);
        }

        @Test
        @DisplayName("Desempate por barcos (incluidas las flotas en tránsito)")
        void desempatePorBarcos() {
            // Ana: 15 en islas + 5 en tránsito = 20; Beto: 17. Producción suma +5 a cada uno.
            EstadoPartida estado = ultimaRonda()
                    .isla(S1, "Ana", 3).isla(I1, "Ana", 3).isla(B12, "Ana", 3).isla(B41, "Ana", 3).isla(I4, "Ana", 3)
                    .flota("Ana", 5, I1, C, 2)
                    .isla(S3, "Beto", 5).isla(I3, "Beto", 3).isla(B23, "Beto", 3).isla(B34, "Beto", 3)
                    .isla(I2, "Beto", 3)
                    .build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.estado().barcosTotales("Ana")).isEqualTo(25);
            assertThat(r.estado().barcosTotales("Beto")).isEqualTo(22);
            assertThat(r.estado().ganadores()).containsExactly("Ana");
        }

        @Test
        @DisplayName("Victoria compartida")
        void victoriaCompartida() {
            EstadoPartida estado = ultimaRonda()
                    .isla(S1, "Ana", 4).isla(I1, "Ana", 2)
                    .isla(S3, "Beto", 3).isla(I3, "Beto", 3)
                    .build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.estado().enCurso()).isFalse();
            assertThat(r.estado().ganadores()).containsExactly("Ana", "Beto");
        }

        @Test
        @DisplayName("Los jugadores eliminados no pueden ganar")
        void eliminadosNoGanan() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto", "Cris").eliminado("Cris").ronda(10).limite(10)
                    .isla(S1, "Ana", 1).isla(S3, "Beto", 1)
                    .build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.estado().ganadores()).containsExactly("Ana", "Beto");
        }

        @Test
        @DisplayName("Antes del límite la partida sigue")
        void antesDelLimite() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto").ronda(24).limite(25)
                    .isla(S1, "Ana", 1).isla(S3, "Beto", 1).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.estado().enCurso()).isTrue();
            assertThat(r.estado().ronda()).isEqualTo(25);
            assertThat(r.informe().ganadores()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Requirement: Partida terminada")
    class PartidaTerminada {

        @Test
        @DisplayName("Órdenes tras el final")
        void ordenesTrasElFinal() {
            Partida partida = Partida.crear(List.of("Ana", "Beto"));
            partida.eliminarPorDesconexion("Beto");

            assertThatThrownBy(() -> partida.enviarOrdenes("Ana", List.of(new Orden(S1, I1, 1))))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("La partida ha terminado");
            assertThatThrownBy(partida::resolverRonda)
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("La partida ha terminado");
            assertThatThrownBy(() -> partida.eliminarPorDesconexion("Ana"))
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("La partida ha terminado");
        }

        @Test
        @DisplayName("Resolver tras terminar por límite de rondas")
        void resolverTrasLimite() {
            Partida partida = Partida.crear(List.of("Ana", "Beto"), 10);
            for (int i = 0; i < 10; i++) {
                partida.resolverRonda();
            }

            assertThat(partida.estado().enCurso()).isFalse();
            assertThat(partida.estado().ganadores()).containsExactly("Ana", "Beto");
            assertThatThrownBy(partida::resolverRonda)
                    .isInstanceOf(ReglaInfringidaException.class)
                    .hasMessageContaining("La partida ha terminado");
        }
    }

    @Nested
    @DisplayName("Requirement: Orden de las fases de resolución")
    class OrdenDeFases {

        @Test
        @DisplayName("Resolución determinista")
        void resolucionDeterminista() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto", "Cris")
                    .isla(S1, "Ana", 6).isla(S2, "Beto", 6).isla(S3, "Cris", 6).isla(I2, "Cris", 2)
                    .llega("Ana", 5, B12, I2).llega("Beto", 5, B23, I2)
                    .flota("Cris", 3, S3, B23, 2)
                    .build();
            Map<String, List<Orden>> ordenes = Map.of(
                    "Ana", List.of(new Orden(S1, I1, 3), new Orden(S1, B12, 2)),
                    "Beto", List.of(new Orden(S2, B12, 4)),
                    "Cris", List.of(new Orden(S3, I3, 6)));

            ResultadoRonda primero = ResolutorRonda.resolver(estado, ordenes);
            ResultadoRonda segundo = ResolutorRonda.resolver(estado, ordenes);

            assertThat(segundo).isEqualTo(primero);
            assertThat(estado.isla(S1).barcos()).as("el estado de partida no se modifica").isEqualTo(6);
        }

        @Test
        @DisplayName("Resolución determinista a través de dos partidas idénticas")
        void deterministaEnPartidas() {
            Partida a = Partida.crear(List.of("Ana", "Beto", "Cris"));
            Partida b = Partida.crear(List.of("Ana", "Beto", "Cris"));
            for (Partida p : List.of(a, b)) {
                p.enviarOrdenes("Cris", List.of(new Orden(S3, I3, 4)));
                p.enviarOrdenes("Ana", List.of(new Orden(S1, I1, 4)));
                p.enviarOrdenes("Beto", List.of(new Orden(S2, I2, 4)));
            }

            assertThat(b.resolverRonda()).isEqualTo(a.resolverRonda());
            assertThat(b.estado()).isEqualTo(a.estado());
        }

        @Test
        @DisplayName("Avance de ronda")
        void avanceDeRonda() {
            Partida partida = Partida.crear(List.of("Ana", "Beto"));
            for (int i = 0; i < 3; i++) {
                partida.resolverRonda();
            }
            assertThat(partida.estado().ronda()).isEqualTo(4);
            partida.enviarOrdenes("Ana", List.of(new Orden(S1, I1, 1)));
            partida.enviarOrdenes("Beto", List.of());

            InformeRonda informe = partida.resolverRonda();

            assertThat(informe.ronda()).isEqualTo(4);
            assertThat(partida.estado().ronda()).isEqualTo(5);
            assertThat(partida.ordenesPendientes()).isEmpty();
        }

        @Test
        @DisplayName("La producción ocurre después del combate y la captura")
        void produccionDespuesDeCombate() {
            // Si la producción fuera antes del combate, Ana defendería con 4 y ganaría.
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto")
                    .isla(S1, "Ana", 3).isla(S3, "Beto", 1).llega("Beto", 4, I1, S1).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado, Map.of());

            assertThat(r.informe().combates().getFirst().ganador()).isEqualTo("Beto");
            assertThat(r.estado().isla(S1)).isEqualTo(new EstadoIsla(S1, "Beto", 1 + 1));
        }

        @Test
        @DisplayName("Las flotas que salen no defienden su isla de origen")
        void salidaAntesDelCombate() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto")
                    .isla(S1, "Ana", 5).isla(S3, "Beto", 1).llega("Beto", 3, I1, S1).build();

            ResultadoRonda r = ResolutorRonda.resolver(estado,
                    Map.of("Ana", List.of(new Orden(S1, B12, 4))));

            assertThat(r.informe().combates().getFirst().fuerzas()).containsEntry("Ana", 1).containsEntry("Beto", 3);
            assertThat(r.estado().isla(S1).dueno()).isEqualTo("Beto");
        }
    }

    @Nested
    @DisplayName("Requirement: Informe de ronda")
    class Informe {

        @Test
        @DisplayName("Informe de una ronda con combate")
        void informeConCombate() {
            EstadoPartida estado = EstadoDePrueba.con("Ana", "Beto", "Cris").ronda(7)
                    .isla(S1, "Ana", 1).isla(S2, "Beto", 1).isla(S3, "Cris", 1).isla(I2, "Cris", 2)
                    .llega("Ana", 5, B12, I2).llega("Beto", 4, B23, I2).build();

            InformeRonda informe = ResolutorRonda.resolver(estado, Map.of()).informe();

            assertThat(informe.ronda()).isEqualTo(7);
            assertThat(informe.combates()).singleElement().satisfies(c -> {
                assertThat(c.isla()).isEqualTo(I2);
                assertThat(c.fuerzas()).containsExactly(
                        Map.entry("Ana", 5), Map.entry("Beto", 4), Map.entry("Cris", 2));
                assertThat(c.ganador()).isEqualTo("Ana");
                assertThat(c.barcosRestantes()).isEqualTo(1);
            });
            assertThat(informe.capturas()).containsExactly(
                    new com.mijuego.archipielago.motor.resolucion.Captura(I2, "Cris", "Ana"));
            assertThat(informe.eliminados()).isEmpty();
            assertThat(informe.ganadores()).isEmpty();
        }

        @Test
        @DisplayName("Informe de una ronda tranquila")
        void informeTranquilo() {
            Partida partida = Partida.crear(List.of("Ana", "Beto"));

            InformeRonda informe = partida.resolverRonda();

            assertThat(informe.ronda()).isEqualTo(1);
            assertThat(informe.combates()).isEmpty();
            assertThat(informe.capturas()).isEmpty();
            assertThat(informe.eliminados()).isEmpty();
            assertThat(informe.ganadores()).isEmpty();
        }
    }
}
