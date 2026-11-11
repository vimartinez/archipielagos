package com.mijuego.archipielago.motor.resolucion;

import com.mijuego.archipielago.motor.ReglaInfringidaException;
import com.mijuego.archipielago.motor.mapa.IslaId;
import com.mijuego.archipielago.motor.mapa.Mapa;
import com.mijuego.archipielago.motor.modelo.EstadoIsla;
import com.mijuego.archipielago.motor.modelo.EstadoPartida;
import com.mijuego.archipielago.motor.modelo.Jugador;
import com.mijuego.archipielago.motor.modelo.Orden;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Valida un conjunto de órdenes contra el estado al inicio de la ronda. La validación es todo o nada:
 * la primera orden inválida rechaza el conjunto completo.
 */
public final class ValidadorOrdenes {

    private ValidadorOrdenes() {
    }

    /**
     * @throws ReglaInfringidaException si la partida ha terminado, el jugador no puede dar órdenes
     *                                  o alguna orden no es válida
     */
    public static void validar(EstadoPartida estado, String apodo, List<Orden> ordenes) {
        if (!estado.enCurso()) {
            throw new ReglaInfringidaException("La partida ha terminado");
        }
        Jugador jugador = estado.jugador(apodo)
                .orElseThrow(() -> new ReglaInfringidaException("El jugador '%s' no está en la partida".formatted(apodo)));
        if (jugador.eliminado()) {
            throw new ReglaInfringidaException("El jugador '%s' está eliminado".formatted(apodo));
        }
        if (ordenes == null) {
            throw new ReglaInfringidaException("El conjunto de órdenes no puede ser nulo");
        }

        Map<IslaId, Integer> salientes = new EnumMap<>(IslaId.class);
        for (int i = 0; i < ordenes.size(); i++) {
            Orden orden = ordenes.get(i);
            if (orden == null || orden.origen() == null || orden.destino() == null) {
                throw new ReglaInfringidaException("Orden %d: debe indicar origen y destino".formatted(i + 1));
            }
            String prefijo = "Orden %d (%s): ".formatted(i + 1, orden.describir());
            if (!estado.isla(orden.origen()).esDe(apodo)) {
                throw new ReglaInfringidaException(prefijo + "%s no es de %s".formatted(orden.origen(), apodo));
            }
            if (!Mapa.hayRuta(orden.origen(), orden.destino())) {
                throw new ReglaInfringidaException(prefijo + "no hay ruta entre %s y %s"
                        .formatted(orden.origen(), orden.destino()));
            }
            if (orden.barcos() < 1) {
                throw new ReglaInfringidaException(prefijo + "hay que enviar al menos 1 barco");
            }
            int total = salientes.merge(orden.origen(), orden.barcos(), Integer::sum);
            EstadoIsla origen = estado.isla(orden.origen());
            if (total > origen.barcos()) {
                throw new ReglaInfringidaException(prefijo + "%s solo tiene %d barcos y se intentan enviar %d"
                        .formatted(orden.origen(), origen.barcos(), total));
            }
        }
    }
}
