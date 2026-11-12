// Conexión WebSocket con el servidor del juego, en el mismo origen que la página.

const ESPERA_LENTA_MS = 3000;

/**
 * Abre la conexión y avisa de su ciclo de vida.
 * @param {{alAbrir: Function, alMensaje: Function, alCerrar: Function, alTardar: Function}} manejadores
 */
export function conectar({ alAbrir, alMensaje, alCerrar, alTardar }) {
    const protocolo = location.protocol === 'https:' ? 'wss' : 'ws';
    const socket = new WebSocket(`${protocolo}://${location.host}/ws`);
    let abierta = false;

    const temporizador = setTimeout(() => {
        if (!abierta) alTardar();
    }, ESPERA_LENTA_MS);

    socket.addEventListener('open', () => {
        abierta = true;
        clearTimeout(temporizador);
        alAbrir();
    });
    socket.addEventListener('message', (evento) => {
        alMensaje(JSON.parse(evento.data));
    });
    socket.addEventListener('close', () => {
        clearTimeout(temporizador);
        alCerrar(abierta);
    });

    return {
        enviar(mensaje) {
            if (socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify(mensaje));
            }
        },
    };
}
