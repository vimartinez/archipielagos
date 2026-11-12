package com.mijuego.archipielago.servidor.sala;

import static org.assertj.core.api.Assertions.assertThat;

import com.mijuego.archipielago.servidor.dto.ErrorDto;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DoblesTest {

    @Test
    void elNotificadorGrabaLosMensajesPorConexion() {
        NotificadorGrabador notificador = new NotificadorGrabador();
        notificador.enviar("ana", new ErrorDto("uno", null));
        notificador.enviar("beto", new ErrorDto("dos", null));
        notificador.enviar("ana", new ErrorDto("tres", null));

        assertThat(notificador.recibidos("ana", ErrorDto.class)).extracting(ErrorDto::mensaje).containsExactly("uno", "tres");
        assertThat(notificador.ultimo("beto", ErrorDto.class).mensaje()).isEqualTo("dos");
    }

    @Test
    void elPlanificadorManualEjecutaSoloLoQueVenceYRespetaCancelaciones() {
        PlanificadorManual planificador = new PlanificadorManual();
        List<String> ejecutadas = new ArrayList<>();
        planificador.programar(Duration.ofSeconds(10), () -> ejecutadas.add("a"));
        Planificador.Tarea cancelada = planificador.programar(Duration.ofSeconds(5), () -> ejecutadas.add("b"));
        planificador.programar(Duration.ofSeconds(60), () -> ejecutadas.add("c"));
        cancelada.cancelar();

        planificador.avanzar(Duration.ofSeconds(30));

        assertThat(ejecutadas).containsExactly("a");
        assertThat(planificador.pendientes()).isEqualTo(1);
    }
}
