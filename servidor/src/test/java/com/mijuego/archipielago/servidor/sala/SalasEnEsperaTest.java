package com.mijuego.archipielago.servidor.sala;

import static org.assertj.core.api.Assertions.assertThat;

import com.mijuego.archipielago.servidor.dto.ErrorDto;
import com.mijuego.archipielago.servidor.dto.MapaDto;
import com.mijuego.archipielago.servidor.dto.PartidaDto;
import com.mijuego.archipielago.servidor.dto.SalaDto;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class SalasEnEsperaTest extends EscenarioSalas {

    @Nested
    class CrearUnaSala {

        @RepeatedTest(20)
        void elCodigoTieneCuatroCaracteresSinAmbiguos() {
            assertThat(new GeneradorCodigos().get()).matches("[A-HJ-NP-Z2-9]{4}");
        }

        @Test
        void salaCreada() {
            gestor.crearSala("ana", "Ana");

            SalaDto sala = vistaSala("Ana");
            assertThat(sala.codigo()).matches("[A-HJ-NP-Z2-9]{4}");
            assertThat(sala.jugadores()).containsExactly("Ana");
            assertThat(sala.anfitrion()).isEqualTo("Ana");
            assertThat(sala.tuApodo()).isEqualTo("Ana");
            assertThat(sala.limiteRondas()).isEqualTo(25);
            assertThat(gestor.existeSala(sala.codigo())).isTrue();
        }

        @Test
        void unCodigoEnUsoSeReintenta() {
            Iterator<String> codigos = List.of("AAAA", "AAAA", "BBBB").iterator();
            GestorSalas conColision = new GestorSalas(notificador, planificador, RONDA, codigos::next);

            conColision.crearSala("ana", "Ana");
            conColision.crearSala("beto", "Beto");

            assertThat(vistaSala("Ana").codigo()).isEqualTo("AAAA");
            assertThat(vistaSala("Beto").codigo()).isEqualTo("BBBB");
        }

        @Test
        void apodoInvalidoAlCrear() {
            gestor.crearSala("x", "   ");
            gestor.crearSala("y", "a".repeat(21));

            assertThat(notificador.ultimo("x", ErrorDto.class).mensaje())
                    .isEqualTo("El apodo debe tener entre 1 y 20 caracteres");
            assertThat(notificador.ultimo("y", ErrorDto.class).peticion()).isEqualTo("crearSala");
            assertThat(gestor.numeroSalas()).isZero();
        }
    }

    @Nested
    class UnirseAUnaSala {

        @Test
        void unionCorrecta() {
            String codigo = sala("Ana");
            gestor.unirse("beto", codigo.toLowerCase(), "  Beto ");

            assertThat(vistaSala("Ana").jugadores()).containsExactly("Ana", "Beto");
            assertThat(vistaSala("Beto").jugadores()).containsExactly("Ana", "Beto");
            assertThat(vistaSala("Beto").tuApodo()).isEqualTo("Beto");
        }

        @Test
        void codigoInexistente() {
            gestor.unirse("beto", "ZZZZ", "Beto");

            assertThat(error("Beto").mensaje()).isEqualTo("La sala no existe");
        }

        @Test
        void apodoRepetidoSinDistinguirMayusculas() {
            String codigo = sala("Ana");
            gestor.unirse("otra", codigo, "ana");

            assertThat(notificador.ultimo("otra", ErrorDto.class).mensaje())
                    .isEqualTo("El apodo 'ana' ya está en uso en la sala");
            assertThat(vistaSala("Ana").jugadores()).containsExactly("Ana");
        }

        @Test
        void salaLlena() {
            String codigo = sala("Ana", "Beto", "Cris", "Dani");
            gestor.unirse("eva", codigo, "Eva");

            assertThat(notificador.ultimo("eva", ErrorDto.class).mensaje()).isEqualTo("La sala está llena");
        }

        @Test
        void partidaYaEmpezada() {
            String codigo = partida("Ana", "Beto");
            gestor.unirse("cris", codigo, "Cris");

            assertThat(error("Cris").mensaje()).isEqualTo("La partida ya ha empezado");
        }

        @Test
        void apodoInvalidoAlUnirse() {
            String codigo = sala("Ana");
            gestor.unirse("beto", codigo, "");

            assertThat(error("Beto").mensaje()).isEqualTo("El apodo debe tener entre 1 y 20 caracteres");
        }

        @Test
        void unaConexionNoPuedeEstarEnDosSalas() {
            sala("Ana");
            String otra = sala("Beto");
            gestor.unirse("ana", otra, "Ana");

            assertThat(error("Ana").mensaje()).isEqualTo("Ya estás en una sala");
        }
    }

    @Nested
    class ConfigurarElLimite {

        @Test
        void anfitrionCambiaElLimite() {
            sala("Ana", "Beto");
            gestor.configurarLimite("ana", 15);

            assertThat(vistaSala("Ana").limiteRondas()).isEqualTo(15);
            assertThat(vistaSala("Beto").limiteRondas()).isEqualTo(15);
        }

        @Test
        void limiteFueraDeRango() {
            sala("Ana");
            gestor.configurarLimite("ana", 60);
            gestor.configurarLimite("ana", 9);

            assertThat(error("Ana").mensaje()).isEqualTo("El límite de rondas debe estar entre 10 y 50");
            assertThat(errores("Ana")).isEqualTo(2);
            assertThat(vistaSala("Ana").limiteRondas()).isEqualTo(25);
        }

        @Test
        void otroJugadorIntentaCambiarlo() {
            sala("Ana", "Beto");
            gestor.configurarLimite("beto", 15);

            assertThat(error("Beto").mensaje()).isEqualTo("Solo el anfitrión puede cambiar el límite de rondas");
            assertThat(vistaSala("Beto").limiteRondas()).isEqualTo(25);
        }
    }

    @Nested
    class EmpezarLaPartida {

        @Test
        void inicioCorrecto() {
            sala("Ana", "Beto", "Cris");
            gestor.configurarLimite("ana", 30);
            gestor.empezar("ana");

            for (String apodo : List.of("Ana", "Beto", "Cris")) {
                PartidaDto partida = estado(apodo);
                assertThat(partida.ronda()).isEqualTo(1);
                assertThat(partida.enCurso()).isTrue();
                assertThat(partida.limiteRondas()).isEqualTo(30);
                assertThat(partida.msRestantes()).isEqualTo(60_000);
                assertThat(partida.tuApodo()).isEqualTo(apodo);
                assertThat(partida.jugadores()).extracting(PartidaDto.JugadorDto::apodo)
                        .containsExactly("Ana", "Beto", "Cris");
                assertThat(notificador.recibidos(conexion(apodo), MapaDto.class)).hasSize(1);
            }
            assertThat(isla("Ana", "S1").dueno()).isEqualTo("Ana");
            assertThat(isla("Ana", "S2").dueno()).isEqualTo("Beto");
            assertThat(isla("Ana", "S3").dueno()).isEqualTo("Cris");
        }

        @Test
        void elMapaLlegaAntesQueElEstado() {
            partida("Ana", "Beto");

            List<String> tipos = notificador.envios().stream()
                    .filter(e -> e.conexion().equals("beto"))
                    .map(e -> e.mensaje().getClass().getSimpleName())
                    .filter(c -> !c.equals("SalaDto"))
                    .toList();
            assertThat(tipos).containsExactly("MapaDto", "PartidaDto");
        }

        @Test
        void unSoloJugador() {
            sala("Ana");
            gestor.empezar("ana");

            assertThat(error("Ana").mensaje()).isEqualTo("Se necesitan al menos 2 jugadores");
        }

        @Test
        void noAnfitrionIntentaEmpezar() {
            sala("Ana", "Beto");
            gestor.empezar("beto");

            assertThat(error("Beto").mensaje()).isEqualTo("Solo el anfitrión puede empezar la partida");
            assertThat(notificador.recibidos("ana", PartidaDto.class)).isEmpty();
        }

        @Test
        void noSePuedeConfigurarTrasEmpezar() {
            partida("Ana", "Beto");
            gestor.configurarLimite("ana", 15);

            assertThat(error("Ana").mensaje()).isEqualTo("La partida ya ha empezado");
        }
    }

    @Nested
    class SalirDeUnaSalaEnEspera {

        @Test
        void saleUnJugador() {
            sala("Ana", "Beto", "Cris");
            gestor.desconectar("beto");

            assertThat(vistaSala("Ana").jugadores()).containsExactly("Ana", "Cris");
            assertThat(vistaSala("Cris").jugadores()).containsExactly("Ana", "Cris");
        }

        @Test
        void saleElAnfitrion() {
            sala("Ana", "Beto", "Cris");
            gestor.desconectar("ana");

            assertThat(vistaSala("Beto").anfitrion()).isEqualTo("Beto");
            assertThat(vistaSala("Cris").anfitrion()).isEqualTo("Beto");

            gestor.empezar("beto");
            assertThat(estado("Cris").jugadores()).extracting(PartidaDto.JugadorDto::apodo)
                    .containsExactly("Beto", "Cris");
        }

        @Test
        void salaVacia() {
            String codigo = sala("Ana", "Beto");
            gestor.desconectar("ana");
            gestor.desconectar("beto");

            assertThat(gestor.existeSala(codigo)).isFalse();
            gestor.unirse("cris", codigo, "Cris");
            assertThat(error("Cris").mensaje()).isEqualTo("La sala no existe");
        }

        @Test
        void desconectarDosVecesNoFalla() {
            sala("Ana", "Beto");
            gestor.desconectar("beto");
            gestor.desconectar("beto");
            gestor.desconectar("desconocida");

            assertThat(vistaSala("Ana").jugadores()).containsExactly("Ana");
        }

        @Test
        void elApodoQuedaLibreAlSalir() {
            String codigo = sala("Ana", "Beto");
            gestor.desconectar("beto");
            gestor.unirse("beto2", codigo, "Beto");

            assertThat(vistaSala("Ana").jugadores()).containsExactly("Ana", "Beto");
            assertThat(planificador.pendientes()).isZero();
            planificador.avanzar(Duration.ofMinutes(5));
        }
    }
}
