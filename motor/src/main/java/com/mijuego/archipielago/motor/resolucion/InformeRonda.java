package com.mijuego.archipielago.motor.resolucion;

import java.util.List;

/**
 * Informe de lo ocurrido al resolver una ronda.
 *
 * @param ronda      ronda resuelta
 * @param combates   combates, en orden de isla
 * @param capturas   islas que cambiaron de dueño, en orden de isla
 * @param eliminados jugadores eliminados en esta ronda, en orden de unión
 * @param ganadores  ganadores si la partida terminó en esta ronda; vacío en otro caso
 */
public record InformeRonda(
        int ronda,
        List<Combate> combates,
        List<Captura> capturas,
        List<String> eliminados,
        List<String> ganadores) {

    public InformeRonda {
        combates = List.copyOf(combates);
        capturas = List.copyOf(capturas);
        eliminados = List.copyOf(eliminados);
        ganadores = List.copyOf(ganadores);
    }

    public boolean partidaTerminada() {
        return !ganadores.isEmpty();
    }
}
