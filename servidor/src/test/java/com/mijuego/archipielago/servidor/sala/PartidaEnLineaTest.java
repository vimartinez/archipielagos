package com.mijuego.archipielago.servidor.sala;

import static org.assertj.core.api.Assertions.assertThat;

import com.mijuego.archipielago.servidor.dto.InformeDto;
import com.mijuego.archipielago.servidor.dto.MensajeSaliente;
import com.mijuego.archipielago.servidor.dto.OrdenDto;
import com.mijuego.archipielago.servidor.dto.OrdenesAceptadasDto;
import com.mijuego.archipielago.servidor.dto.PartidaDto;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PartidaEnLineaTest extends EscenarioSalas {

    private PartidaDto.JugadorDto jugador(String vistoPor, String apodo) {
        return estado(vistoPor).jugadores().stream().filter(j -> j.apodo().equals(apodo)).findFirst().orElseThrow();
    }

    private List<PartidaDto.FlotaDto> flotasDe(String vistoPor, String apodo) {
        return estado(vistoPor).flotas().stream().filter(f -> f.dueno().equals(apodo)).toList();
    }

    @Nested
    class EnvioDeOrdenes {

        @Test
        void ordenesAceptadasYSecretas() {
            partida("Ana", "Beto");
            ordenes("Ana", orden(3, "S1", "I1"));

            assertThat(notificador.ultimo("ana", OrdenesAceptadasDto.class).ordenes())
                    .containsExactly(orden(3, "S1", "I1"));
            assertThat(notificador.recibidos("beto", OrdenesAceptadasDto.class)).isEmpty();
        }

        @Test
        void ordenesRechazadasConservanLasVigentes() {
            partida("Ana", "Beto");
            ordenes("Ana", orden(3, "S1", "I1"));
            ordenes("Ana", orden(9, "S1", "I1"));

            assertThat(error("Ana").mensaje()).contains("S1 solo tiene 5 barcos");
            assertThat(error("Ana").peticion()).isEqualTo("ordenes");
            assertThat(errores("Beto")).isZero();

            confirmar("Ana", "Beto");
            assertThat(isla("Ana", "I1")).isEqualTo(new PartidaDto.IslaDto("I1", "Ana", 4));
            assertThat(isla("Ana", "S1")).isEqualTo(new PartidaDto.IslaDto("S1", "Ana", 3));
        }

        @Test
        void islaDesconocida() {
            partida("Ana", "Beto");
            ordenes("Ana", orden(1, "S9", "I1"));

            assertThat(error("Ana").mensaje()).isEqualTo("Isla desconocida: S9");
        }

        @Test
        void ordenesAntesDeEmpezar() {
            sala("Ana", "Beto");
            ordenes("Ana", orden(1, "S1", "I1"));
            ordenes("Cris", orden(1, "S1", "I1"));

            assertThat(error("Ana").mensaje()).isEqualTo("La partida no ha empezado");
            assertThat(error("Cris").mensaje()).isEqualTo("No estás en ninguna sala");
        }
    }

    @Nested
    class ConfirmacionDeOrdenes {

        @Test
        void confirmarSinOrdenes() {
            partida("Ana", "Beto", "Cris");
            confirmar("Beto");

            assertThat(estado("Ana").confirmados()).containsExactly("Beto");
            assertThat(estado("Cris").confirmados()).containsExactly("Beto");
        }

        @Test
        void cambiarOrdenesTrasConfirmar() {
            partida("Ana", "Beto", "Cris");
            confirmar("Ana");
            ordenes("Ana", orden(1, "S1", "I1"));

            assertThat(error("Ana").mensaje()).isEqualTo("Ya confirmaste tus órdenes para esta ronda");
            assertThat(notificador.recibidos("ana", OrdenesAceptadasDto.class)).isEmpty();
        }

        @Test
        void confirmarDosVeces() {
            partida("Ana", "Beto", "Cris");
            confirmar("Ana", "Ana");

            assertThat(error("Ana").mensaje()).isEqualTo("Ya confirmaste tus órdenes para esta ronda");
        }

        @Test
        void losDemasNoVenLasOrdenes() {
            partida("Ana", "Beto");
            ordenes("Ana", orden(3, "S1", "I1"));
            confirmar("Ana");

            assertThat(notificador.envios()).filteredOn(e -> e.conexion().equals("beto"))
                    .extracting(NotificadorGrabador.Envio::mensaje)
                    .noneMatch(m -> m instanceof OrdenesAceptadasDto);
        }
    }

    @Nested
    class RelojDeLaRonda {

        @Test
        void venceElTiempo() {
            partida("Ana", "Beto", "Cris");
            ordenes("Beto", orden(2, "S2", "B23"));

            planificador.avanzar(Duration.ofSeconds(59));
            assertThat(estado("Ana").ronda()).isEqualTo(1);

            planificador.avanzar(Duration.ofSeconds(1));
            assertThat(notificador.ultimo("ana", InformeDto.class).ronda()).isEqualTo(1);
            PartidaDto ronda2 = estado("Ana");
            assertThat(ronda2.ronda()).isEqualTo(2);
            assertThat(ronda2.msRestantes()).isEqualTo(60_000);
            assertThat(ronda2.confirmados()).isEmpty();
            assertThat(flotasDe("Ana", "Beto"))
                    .containsExactly(new PartidaDto.FlotaDto("Beto", 2, "S2", "B23", 2, 3));
        }

        @Test
        void jugadorSinOrdenesAlVencer() {
            partida("Ana", "Beto", "Cris");
            planificador.avanzar(RONDA);

            assertThat(flotasDe("Ana", "Cris")).isEmpty();
            assertThat(isla("Ana", "S3")).isEqualTo(new PartidaDto.IslaDto("S3", "Cris", 6));
        }

        @Test
        void elRestanteSeCalculaDesdeElInicioDeLaRonda() {
            partida("Ana", "Beto", "Cris");
            planificador.avanzar(Duration.ofSeconds(15));
            confirmar("Ana");

            assertThat(estado("Beto").msRestantes()).isEqualTo(45_000);
        }
    }

    @Nested
    class ResolucionAnticipada {

        @Test
        void todosConfirman() {
            partida("Ana", "Beto", "Cris");
            planificador.avanzar(Duration.ofSeconds(20));
            confirmar("Ana", "Beto", "Cris");

            assertThat(estado("Ana").ronda()).isEqualTo(2);
            assertThat(estado("Ana").msRestantes()).isEqualTo(60_000);

            // el plazo de la ronda 1 (a los 60 s) no debe resolver la ronda 2
            planificador.avanzar(Duration.ofSeconds(40));
            assertThat(estado("Ana").ronda()).isEqualTo(2);
            assertThat(notificador.recibidos("ana", InformeDto.class)).hasSize(1);

            planificador.avanzar(Duration.ofSeconds(20));
            assertThat(estado("Ana").ronda()).isEqualTo(3);
        }

        @Test
        void faltaUnoPorConfirmar() {
            partida("Ana", "Beto", "Cris");
            confirmar("Ana", "Beto");

            assertThat(estado("Cris").ronda()).isEqualTo(1);
            assertThat(estado("Cris").confirmados()).containsExactly("Ana", "Beto");
            assertThat(notificador.recibidos("cris", InformeDto.class)).isEmpty();
        }
    }

    @Nested
    class DifusionDelEstadoYDelInforme {

        @Test
        void rondaResueltaConCombate() {
            partida("Ana", "Beto", "Cris");
            ordenes("Ana", orden(5, "S1", "B12"));
            ordenes("Beto", orden(3, "S2", "B12"));
            for (int i = 0; i < 3; i++) {
                confirmar("Ana", "Beto", "Cris");
            }

            for (String apodo : List.of("Ana", "Beto", "Cris")) {
                InformeDto informe = notificador.ultimo(conexion(apodo), InformeDto.class);
                assertThat(informe.ronda()).isEqualTo(3);
                assertThat(informe.combates()).containsExactly(new InformeDto.CombateDto("B12",
                        List.of(new InformeDto.FuerzaDto("Ana", 5), new InformeDto.FuerzaDto("Beto", 3)), "Ana", 2));
                assertThat(informe.capturas()).containsExactly(new InformeDto.CapturaDto("B12", null, "Ana"));
                assertThat(estado(apodo).ronda()).isEqualTo(4);
            }
        }

        @Test
        void elInformeLlegaAntesQueElEstadoDeLaRondaSiguiente() {
            partida("Ana", "Beto");
            notificador.limpiar();
            confirmar("Ana", "Beto");

            List<MensajeSaliente> recibidos = notificador.envios().stream()
                    .filter(e -> e.conexion().equals("beto")).map(NotificadorGrabador.Envio::mensaje).toList();
            // la confirmación de Ana se difunde; la de Beto resuelve: informe y después el estado nuevo
            assertThat(recibidos).hasSize(3);
            assertThat(recibidos.get(0)).isInstanceOf(PartidaDto.class);
            assertThat(recibidos.get(1)).isInstanceOf(InformeDto.class);
            assertThat(recibidos.get(2)).isInstanceOf(PartidaDto.class);
        }

        @Test
        void cadaConfirmacionSeDifunde() {
            partida("Ana", "Beto", "Cris");
            int antes = notificador.recibidos("cris", PartidaDto.class).size();
            confirmar("Ana");

            assertThat(notificador.recibidos("cris", PartidaDto.class)).hasSize(antes + 1);
        }
    }

    @Nested
    class EliminacionPorDesconexion {

        @Test
        void unJugadorCierraLaPestana() {
            partida("Ana", "Beto", "Cris");
            ordenes("Cris", orden(2, "S3", "B23"));
            confirmar("Ana", "Beto", "Cris");
            gestor.desconectar("cris");

            assertThat(jugador("Ana", "Cris").activo()).isFalse();
            assertThat(isla("Ana", "S3")).isEqualTo(new PartidaDto.IslaDto("S3", null, 0));
            assertThat(flotasDe("Ana", "Cris")).isEmpty();
            assertThat(estado("Beto").enCurso()).isTrue();
        }

        @Test
        void elQueFaltabaPorConfirmarSeDesconecta() {
            partida("Ana", "Beto", "Cris");
            confirmar("Ana", "Beto");
            gestor.desconectar("cris");

            assertThat(notificador.ultimo("ana", InformeDto.class).ronda()).isEqualTo(1);
            assertThat(estado("Ana").ronda()).isEqualTo(2);
        }

        @Test
        void quedaUnSoloJugador() {
            partida("Ana", "Beto");
            gestor.desconectar("beto");

            PartidaDto fin = estado("Ana");
            assertThat(fin.enCurso()).isFalse();
            assertThat(fin.ganadores()).containsExactly("Ana");
            assertThat(fin.msRestantes()).isZero();
            assertThat(planificador.pendientes()).isZero();
        }

        @Test
        void sinPosibilidadDeVolver() {
            String codigo = partida("Ana", "Beto", "Cris");
            gestor.desconectar("cris");
            gestor.unirse("cris", codigo, "Cris");

            assertThat(error("Cris").mensaje()).isEqualTo("La partida ya ha empezado");
        }
    }

    @Nested
    class Espectadores {

        /**
         * Ana conquista S2, la única isla de Beto, por el camino S1 - I1 - B12 - I2 - S2 (todas las rutas
         * de distancia 1), acumulando barcos en I2 antes del asalto. Beto y Cris no mueven nada.
         */
        private void anaEliminaABeto() {
            Map<String, String> siguiente = Map.of("S1", "I1", "I1", "B12", "B12", "I2");
            for (int ronda = 0; ronda < 40 && jugador("Ana", "Beto").activo(); ronda++) {
                PartidaDto estado = estado("Ana");
                int defensaS2 = isla("Ana", "S2").barcos();
                List<OrdenDto> ordenes = new ArrayList<>();
                for (PartidaDto.IslaDto isla : estado.islas()) {
                    if (!"Ana".equals(isla.dueno()) || isla.barcos() == 0) {
                        continue;
                    }
                    if (siguiente.containsKey(isla.id())) {
                        ordenes.add(orden(isla.barcos(), isla.id(), siguiente.get(isla.id())));
                    } else if (isla.id().equals("I2") && isla.barcos() > defensaS2 + 2) {
                        ordenes.add(orden(isla.barcos(), "I2", "S2"));
                    }
                }
                gestor.enviarOrdenes("ana", ordenes);
                confirmar("Ana", "Beto", "Cris");
            }
            assertThat(jugador("Ana", "Beto").activo()).as("Beto debería estar eliminado").isFalse();
        }

        @Test
        void eliminadoQueSigueMirando() {
            partida("Ana", "Beto", "Cris");
            anaEliminaABeto();
            int ronda = estado("Beto").ronda();

            ordenes("Beto", orden(1, "S2", "I2"));
            assertThat(error("Beto").mensaje()).isEqualTo("El jugador 'Beto' está eliminado");
            confirmar("Beto");
            assertThat(error("Beto").mensaje()).isEqualTo("Estás eliminado: no puedes confirmar órdenes");

            confirmar("Ana", "Cris");
            assertThat(notificador.ultimo("beto", InformeDto.class).ronda()).isEqualTo(ronda);
            assertThat(estado("Beto").ronda()).isEqualTo(ronda + 1);
            assertThat(estado("Beto").enCurso()).isTrue();
        }

        @Test
        void laDesconexionDeUnEspectadorNoAfecta() {
            partida("Ana", "Beto", "Cris");
            anaEliminaABeto();
            int mensajesAna = notificador.recibidos("ana", PartidaDto.class).size();

            gestor.desconectar("beto");

            assertThat(notificador.recibidos("ana", PartidaDto.class)).hasSize(mensajesAna);
            confirmar("Ana", "Cris");
            assertThat(estado("Ana").enCurso()).isTrue();
        }
    }

    @Nested
    class FinDeLaPartida {

        @Test
        void finPorLimiteDeRondas() {
            sala("Ana", "Beto");
            gestor.configurarLimite("ana", 10);
            gestor.empezar("ana");
            for (int i = 0; i < 10; i++) {
                confirmar("Ana", "Beto");
            }

            for (String apodo : List.of("Ana", "Beto")) {
                assertThat(notificador.ultimo(conexion(apodo), InformeDto.class).ronda()).isEqualTo(10);
                assertThat(notificador.ultimo(conexion(apodo), InformeDto.class).ganadores())
                        .containsExactly("Ana", "Beto");
                PartidaDto fin = estado(apodo);
                assertThat(fin.enCurso()).isFalse();
                assertThat(fin.ganadores()).containsExactly("Ana", "Beto");
                assertThat(fin.msRestantes()).isZero();
            }
            assertThat(planificador.pendientes()).isZero();

            ordenes("Ana", orden(1, "S1", "I1"));
            assertThat(error("Ana").mensaje()).isEqualTo("La partida ha terminado");
            confirmar("Beto");
            assertThat(error("Beto").mensaje()).isEqualTo("La partida ha terminado");

            planificador.avanzar(Duration.ofMinutes(10));
            assertThat(estado("Ana").ronda()).isEqualTo(10);
        }

        @Test
        void laSalaSeEliminaAlIrseElUltimoTrasElFin() {
            String codigo = partida("Ana", "Beto");
            gestor.desconectar("beto");
            assertThat(gestor.existeSala(codigo)).isTrue();

            gestor.desconectar("ana");
            assertThat(gestor.existeSala(codigo)).isFalse();
        }

        @Test
        void todosSeVan() {
            String codigo = partida("Ana", "Beto", "Cris");
            gestor.desconectar("ana");
            gestor.desconectar("beto");
            gestor.desconectar("cris");

            assertThat(gestor.existeSala(codigo)).isFalse();
            assertThat(planificador.pendientes()).isZero();
        }
    }
}
