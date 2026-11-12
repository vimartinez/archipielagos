// Dibujo del mapa en SVG: islas en posiciones fijas y simétricas, rutas con su distancia y flotas en tránsito.

const SVG = 'http://www.w3.org/2000/svg';

export const COLORES = ['#d1495b', '#2e86de', '#1b998b', '#8e44ad'];
const COLOR_NEUTRAL = '#b8b8b8';

// Posiciones de dibujo. Índices 1..4 en el sentido de las agujas del reloj desde arriba a la izquierda.
const POSICIONES = {
    S1: [-250, -250], S2: [250, -250], S3: [250, 250], S4: [-250, 250],
    B12: [0, -300], B23: [300, 0], B34: [0, 300], B41: [-300, 0],
    I1: [-110, -110], I2: [110, -110], I3: [110, 110], I4: [-110, 110],
    C: [0, 0],
};
const RADIO_ISLA = 34;

function el(nombre, atributos = {}, ...hijos) {
    const nodo = document.createElementNS(SVG, nombre);
    for (const [clave, valor] of Object.entries(atributos)) nodo.setAttribute(clave, valor);
    for (const hijo of hijos) nodo.append(hijo);
    return nodo;
}

/** Color de un jugador según su orden de llegada a la partida. */
export function colorDe(apodo, jugadores) {
    const indice = jugadores.findIndex((j) => j.apodo === apodo);
    return indice < 0 ? COLOR_NEUTRAL : COLORES[indice % COLORES.length];
}

/** Islas conectadas directamente con {@code isla}. */
export function vecinas(mapa, isla) {
    return mapa.rutas
        .filter((r) => r.a === isla || r.b === isla)
        .map((r) => (r.a === isla ? r.b : r.a));
}

/**
 * Redibuja el mapa completo.
 * @param {SVGElement} svg
 * @param {object} mapa      mensaje "mapa" del servidor (islas y rutas)
 * @param {object} partida   mensaje "partida" del servidor
 * @param {{seleccionada: string|null, destinos: string[], alPulsarIsla: Function}} interaccion
 */
export function dibujarMapa(svg, mapa, partida, { seleccionada, destinos, alPulsarIsla }) {
    svg.replaceChildren();

    const capaRutas = el('g');
    const capaIslas = el('g');
    const capaFlotas = el('g');
    svg.append(capaRutas, capaIslas, capaFlotas);

    for (const ruta of mapa.rutas) {
        const [x1, y1] = POSICIONES[ruta.a];
        const [x2, y2] = POSICIONES[ruta.b];
        capaRutas.append(el('line', { x1, y1, x2, y2, class: 'ruta' }));
        const etiqueta = el('g', { class: 'distancia', transform: `translate(${(x1 + x2) / 2} ${(y1 + y2) / 2})` },
            el('circle', { r: 11 }),
            texto(String(ruta.distancia)));
        etiqueta.append(titulo(`${ruta.a}–${ruta.b}: ${ruta.distancia} ${ruta.distancia === 1 ? 'ronda' : 'rondas'}`));
        capaRutas.append(etiqueta);
    }

    for (const isla of partida.islas) {
        const [x, y] = POSICIONES[isla.id];
        const clases = ['isla'];
        if (!isla.dueno) clases.push('neutral');
        if (isla.id === seleccionada) clases.push('seleccionada');
        if (destinos.includes(isla.id)) clases.push('destino-posible');
        const grupo = el('g', { class: clases.join(' '), transform: `translate(${x} ${y})` },
            el('circle', { r: RADIO_ISLA, fill: isla.dueno ? colorDe(isla.dueno, partida.jugadores) : COLOR_NEUTRAL }),
            texto(String(isla.barcos), 'barcos'),
            texto(isla.id, 'nombre', { y: -RADIO_ISLA - 8 }));
        grupo.append(titulo(`${isla.id}: ${isla.dueno ?? 'neutral'}, ${isla.barcos} barcos`));
        grupo.addEventListener('click', () => alPulsarIsla(isla.id));
        capaIslas.append(grupo);
    }

    // Flotas: sobre su ruta, en proporción a lo recorrido; las que comparten tramo se separan un poco.
    const ocupados = new Map();
    for (const flota of partida.flotas) {
        const [x1, y1] = POSICIONES[flota.origen];
        const [x2, y2] = POSICIONES[flota.destino];
        const avance = (flota.distancia - flota.rondasRestantes) / flota.distancia;
        const clave = `${flota.origen}-${flota.destino}-${flota.rondasRestantes}`;
        const repetidas = ocupados.get(clave) ?? 0;
        ocupados.set(clave, repetidas + 1);
        const largo = Math.hypot(x2 - x1, y2 - y1);
        const [nx, ny] = [-(y2 - y1) / largo, (x2 - x1) / largo];
        const desplazamiento = 16 + repetidas * 30;
        const x = x1 + (x2 - x1) * avance + nx * desplazamiento;
        const y = y1 + (y2 - y1) * avance + ny * desplazamiento;
        const grupo = el('g', { class: 'flota', transform: `translate(${x} ${y})` },
            el('rect', { x: -26, y: -13, width: 52, height: 26, rx: 8, fill: colorDe(flota.dueno, partida.jugadores) }),
            texto(`${flota.barcos} · ${flota.rondasRestantes}r`));
        grupo.append(titulo(`${flota.dueno}: ${flota.barcos} barcos de ${flota.origen} a ${flota.destino}, `
            + `llegan en ${flota.rondasRestantes} ${flota.rondasRestantes === 1 ? 'ronda' : 'rondas'}`));
        capaFlotas.append(grupo);
    }
}

function texto(contenido, clase, atributos = {}) {
    const nodo = el('text', { ...(clase ? { class: clase } : {}), ...atributos });
    nodo.textContent = contenido;
    return nodo;
}

function titulo(contenido) {
    const nodo = el('title');
    nodo.textContent = contenido;
    return nodo;
}
