package com.mijuego.archipielago.servidor.sala;

import com.mijuego.archipielago.servidor.dto.MensajeSaliente;

/** Puerto de salida del núcleo: entrega mensajes a la conexión de un jugador. */
public interface Notificador {

    /** Envía un mensaje a una conexión. Si la conexión ya no existe, el mensaje se descarta. */
    void enviar(String conexion, MensajeSaliente mensaje);
}
