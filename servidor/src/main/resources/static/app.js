// Cliente de Archipiélago: máquina de pantallas (inicio, espera, partida, fin) sobre la conexión WebSocket.

import { conectar } from './conexion.js';
import { colorDe, dibujarMapa } from './mapa.js';
import { Ordenes } from './ordenes.js';

const $ = (id) => document.getElementById(id);

const PANTALLAS = ['inicio', 'espera', 'partida', 'desconectado'];
const REINTENTO_MS = 2000;

const estado = {
    conectado: false,
    sala: null,
    mapa: null,
    partida: null,
    informe: null,
    venceEn: 0,
    seleccionada: null,
};

let conexion;
const ordenes = new Ordenes({
    enviar: (lista) => conexion.enviar({ tipo: 'ordenes', ordenes: lista }),
    alCambiar: () => pintarPartida(),
});

// ------------------------------------------------------------------ utilidades

function mostrar(pantalla) {
    for (const p of PANTALLAS) $(`pantalla-${p}`).hidden = p !== pantalla;
}

function elemento(etiqueta, propiedades = {}, ...hijos) {
    const nodo = document.createElement(etiqueta);
    Object.assign(nodo, propiedades);
    nodo.append(...hijos);
    return nodo;
}

function muestraColor(apodo) {
    const color = estado.partida ? colorDe(apodo, estado.partida.jugadores) : '#999';
    return elemento('span', { className: 'muestra', style: `background:${color}` });
}

function plural(n, singular, varios) {
    return `${n} ${n === 1 ? singular : varios}`;
}

// ------------------------------------------------------------------ conexión

function iniciarConexion() {
    $('estado-conexion').textContent = 'Conectando con el servidor…';
    conexion = conectar({
        alAbrir() {
            estado.conectado = true;
            $('estado-conexion').textContent = 'Conectado';
            $('estado-conexion').classList.remove('lento');
            $('boton-crear').disabled = false;
            $('boton-unirse').disabled = false;
        },
        alTardar() {
            $('estado-conexion').textContent =
                'El servidor está despertando; la primera conexión puede tardar alrededor de un minuto…';
            $('estado-conexion').classList.add('lento');
        },
        alCerrar(llegoAAbrirse) {
            if (!llegoAAbrirse) {
                // El servidor aún no responde (por ejemplo, mientras despierta): se reintenta.
                $('estado-conexion').classList.add('lento');
                $('estado-conexion').textContent =
                    'El servidor está despertando; la primera conexión puede tardar alrededor de un minuto…';
                setTimeout(iniciarConexion, REINTENTO_MS);
                return;
            }
            estado.conectado = false;
            $('estado-conexion').textContent = 'Desconectado';
            mostrar('desconectado');
        },
        alMensaje: recibir,
    });
}

function recibir(mensaje) {
    switch (mensaje.tipo) {
        case 'sala':
            estado.sala = mensaje;
            pintarEspera();
            mostrar('espera');
            break;
        case 'mapa':
            estado.mapa = mensaje;
            break;
        case 'partida':
            estado.partida = mensaje;
            estado.venceEn = performance.now() + mensaje.msRestantes;
            if (mensaje.ronda !== ordenes.ronda) $('error-ordenes').textContent = '';
            ordenes.nuevaRonda(mensaje.ronda);
            if (!mensaje.enCurso) estado.seleccionada = null;
            mostrar('partida');
            pintarPartida();
            break;
        case 'informe':
            estado.informe = mensaje;
            pintarInforme();
            break;
        case 'ordenesAceptadas':
            $('error-ordenes').textContent = '';
            ordenes.aceptar(mensaje.ordenes);
            break;
        case 'error':
            mostrarError(mensaje);
            break;
    }
}

function mostrarError({ mensaje, peticion }) {
    switch (peticion) {
        case 'crearSala':
        case 'unirse':
            $('error-inicio').textContent = mensaje;
            break;
        case 'configurarLimite':
        case 'empezar':
            $('error-espera').textContent = mensaje;
            if (estado.sala) $('limite').value = estado.sala.limiteRondas;
            break;
        case 'ordenes':
            $('error-ordenes').textContent = mensaje;
            ordenes.rechazar();
            break;
        default:
            if (estado.partida) $('error-ordenes').textContent = mensaje;
            else if (estado.sala) $('error-espera').textContent = mensaje;
            else $('error-inicio').textContent = mensaje;
    }
}

// ------------------------------------------------------------------ inicio

function prepararInicio() {
    $('boton-crear').addEventListener('click', () => {
        $('error-inicio').textContent = '';
        conexion.enviar({ tipo: 'crearSala', apodo: $('apodo').value });
    });
    const unirse = () => {
        $('error-inicio').textContent = '';
        conexion.enviar({ tipo: 'unirse', codigo: $('codigo').value, apodo: $('apodo').value });
    };
    $('boton-unirse').addEventListener('click', unirse);
    $('codigo').addEventListener('keydown', (e) => { if (e.key === 'Enter') unirse(); });
    $('codigo').addEventListener('input', () => { $('codigo').value = $('codigo').value.toUpperCase(); });
}

// ------------------------------------------------------------------ sala de espera

function soyAnfitrion() {
    return estado.sala && estado.sala.anfitrion === estado.sala.tuApodo;
}

function prepararEspera() {
    $('boton-copiar').addEventListener('click', async () => {
        try {
            await navigator.clipboard.writeText(estado.sala.codigo);
            $('boton-copiar').textContent = '¡Copiado!';
            setTimeout(() => { $('boton-copiar').textContent = 'Copiar'; }, 1500);
        } catch {
            $('boton-copiar').textContent = 'Cópialo a mano';
        }
    });
    $('limite').addEventListener('change', () => {
        $('error-espera').textContent = '';
        conexion.enviar({ tipo: 'configurarLimite', limiteRondas: Number($('limite').value) });
    });
    $('boton-empezar').addEventListener('click', () => {
        $('error-espera').textContent = '';
        conexion.enviar({ tipo: 'empezar' });
    });
}

function pintarEspera() {
    const sala = estado.sala;
    const anfitrion = soyAnfitrion();
    $('codigo-sala').textContent = sala.codigo;
    $('cuenta-jugadores').textContent = `(${sala.jugadores.length}/4)`;
    $('lista-espera').replaceChildren(...sala.jugadores.map((apodo) => elemento('li', {},
        apodo,
        ...(apodo === sala.anfitrion ? [elemento('span', { className: 'marca', textContent: '★ anfitrión' })] : []),
        ...(apodo === sala.tuApodo ? [elemento('span', { className: 'marca', textContent: '(tú)' })] : []))));

    $('limite').hidden = !anfitrion;
    $('limite-texto').hidden = anfitrion;
    if (document.activeElement !== $('limite')) $('limite').value = sala.limiteRondas;
    $('limite-texto').textContent = sala.limiteRondas;
    $('boton-empezar').hidden = !anfitrion;
    $('boton-empezar').disabled = sala.jugadores.length < 2;
    $('espera-invitado').hidden = anfitrion;
}

// ------------------------------------------------------------------ partida

function yo() {
    return estado.partida.jugadores.find((j) => j.apodo === estado.partida.tuApodo);
}

function puedoOrdenar() {
    const p = estado.partida;
    return p.enCurso && yo()?.activo && !p.confirmados.includes(p.tuApodo);
}

function prepararPartida() {
    $('orden-origen').addEventListener('change', () => {
        estado.seleccionada = $('orden-origen').value || null;
        pintarPartida();
    });
    $('boton-anadir').addEventListener('click', () => {
        const origen = $('orden-origen').value;
        const destino = $('orden-destino').value;
        const barcos = Number($('orden-barcos').value);
        if (!origen || !destino || !Number.isInteger(barcos) || barcos < 1) {
            $('error-ordenes').textContent = 'Elige origen, destino y un número de barcos.';
            return;
        }
        const disponibles = ordenes.disponibles(estado.partida, origen);
        if (barcos > disponibles) {
            $('error-ordenes').textContent = `${origen} solo tiene ${plural(disponibles, 'barco disponible', 'barcos disponibles')}.`;
            return;
        }
        $('error-ordenes').textContent = '';
        estado.seleccionada = null;
        ordenes.anadir({ origen, destino, barcos });
    });
    $('boton-confirmar').addEventListener('click', () => conexion.enviar({ tipo: 'confirmar' }));
    for (const boton of document.querySelectorAll('.boton-volver')) {
        boton.addEventListener('click', () => location.reload());
    }
    setInterval(pintarCuentaAtras, 250);
}

function alPulsarIsla(isla) {
    if (!puedoOrdenar()) return;
    const p = estado.partida;
    const origenes = ordenes.origenesPosibles(p);
    if (estado.seleccionada && ordenes.destinosPosibles(estado.mapa, estado.seleccionada).includes(isla)) {
        // Segundo clic en un destino: se rellena el formulario con todos los barcos disponibles.
        $('orden-origen').value = estado.seleccionada;
        pintarFormulario();
        $('orden-destino').value = isla;
        $('orden-barcos').value = ordenes.disponibles(p, estado.seleccionada);
        origenFormulario = estado.seleccionada;
        $('orden-barcos').focus();
        return;
    }
    estado.seleccionada = origenes.includes(isla) && estado.seleccionada !== isla ? isla : null;
    pintarPartida();
}

function pintarPartida() {
    const p = estado.partida;
    if (!p || !estado.mapa) return;

    const destinos = puedoOrdenar() ? ordenes.destinosPosibles(estado.mapa, estado.seleccionada) : [];
    dibujarMapa($('mapa'), estado.mapa, p, { seleccionada: estado.seleccionada, destinos, alPulsarIsla });

    $('texto-ronda').textContent = `Ronda ${p.ronda} / ${p.limiteRondas}`;
    pintarCuentaAtras();
    $('lista-partida').replaceChildren(...p.jugadores.map((j) => elemento('li', { className: j.activo ? '' : 'eliminado' },
        muestraColor(j.apodo),
        j.apodo,
        ...(j.apodo === p.tuApodo ? [elemento('span', { className: 'marca', textContent: '(tú)' })] : []),
        elemento('span', { className: 'marca', textContent: `${plural(j.islas, 'isla', 'islas')}, ${plural(j.barcos, 'barco', 'barcos')}` }),
        ...(p.confirmados.includes(j.apodo) ? [elemento('span', { className: 'confirmado', title: 'Ha confirmado', textContent: '✓' })] : []))));

    const activo = yo()?.activo;
    $('aviso-eliminado').hidden = !p.enCurso || activo;
    $('panel-ordenes').hidden = !p.enCurso || !activo;
    $('panel-fin').hidden = p.enCurso;
    if (p.enCurso && activo) pintarOrdenes();
    if (!p.enCurso) pintarFin();
}

function pintarCuentaAtras() {
    const p = estado.partida;
    if (!p) return;
    if (!p.enCurso) {
        $('cuenta-atras').textContent = '';
        return;
    }
    const segundos = Math.max(0, Math.ceil((estado.venceEn - performance.now()) / 1000));
    $('cuenta-atras').textContent = `${segundos} s`;
    $('cuenta-atras').classList.toggle('urgente', segundos <= 10);
}

function opcion(valor, texto) {
    return elemento('option', { value: valor, textContent: texto });
}

function pintarFormulario() {
    const p = estado.partida;
    const origen = $('orden-origen');
    const elegido = estado.seleccionada ?? origen.value;
    const origenes = ordenes.origenesPosibles(p);
    origen.replaceChildren(opcion('', '—'),
        ...origenes.map((id) => opcion(id, `${id} (${ordenes.disponibles(p, id)})`)));
    origen.value = origenes.includes(elegido) ? elegido : '';

    const destino = $('orden-destino');
    const destinoPrevio = destino.value;
    const destinos = ordenes.destinosPosibles(estado.mapa, origen.value);
    destino.replaceChildren(opcion('', '—'), ...destinos.map((id) => opcion(id, id)));
    destino.value = destinos.includes(destinoPrevio) ? destinoPrevio : '';

    // El número de barcos solo se propone al cambiar de origen: un repintado no debe tocar lo que el
    // jugador está escribiendo (el exceso se valida al pulsar "Añadir").
    const maximo = origen.value ? ordenes.disponibles(p, origen.value) : 0;
    $('orden-barcos').max = maximo;
    if (origen.value !== origenFormulario) {
        $('orden-barcos').value = origen.value ? maximo : '';
        origenFormulario = origen.value;
    }
}

let origenFormulario = '';

function pintarOrdenes() {
    const bloqueado = !puedoOrdenar();
    pintarFormulario();
    for (const id of ['orden-origen', 'orden-destino', 'orden-barcos', 'boton-anadir', 'boton-confirmar']) {
        $(id).disabled = bloqueado;
    }
    $('texto-confirmado').hidden = !estado.partida.confirmados.includes(estado.partida.tuApodo);

    const lista = ordenes.lista;
    $('lista-ordenes').replaceChildren(...(lista.length === 0
        ? [elemento('li', { className: 'tenue', textContent: 'Sin órdenes: esta ronda no moverás barcos.' })]
        : lista.map((o, i) => elemento('li', {},
            `${o.barcos} de ${o.origen} a ${o.destino}`,
            elemento('button', {
                textContent: '✕', title: 'Quitar', disabled: bloqueado,
                onclick: () => ordenes.quitar(i),
            })))));
}

function pintarInforme() {
    const inf = estado.informe;
    const lineas = [];
    for (const c of inf.combates) {
        const fuerzas = c.fuerzas.map((f) => `${f.apodo} ${f.barcos}`).join(' contra ');
        const resultado = c.ganador
            ? `gana ${c.ganador} con ${plural(c.barcosRestantes, 'barco', 'barcos')}`
            : 'sin ganador: la isla no cambia de dueño';
        lineas.push(`${c.isla}: ${fuerzas}, ${resultado}.`);
    }
    for (const c of inf.capturas) {
        lineas.push(c.duenoAnterior
            ? `${c.nuevoDueno} capturó ${c.isla} a ${c.duenoAnterior}.`
            : `${c.nuevoDueno} capturó ${c.isla}.`);
    }
    for (const e of inf.eliminados) lineas.push(`${e} ha sido eliminado.`);
    if (lineas.length === 0) lineas.push('Ronda tranquila: sin combates ni capturas.');

    $('informe').replaceChildren(
        elemento('p', {}, elemento('strong', { textContent: `Ronda ${inf.ronda}` })),
        ...lineas.map((l) => elemento('p', { textContent: l })));
}

function pintarFin() {
    const p = estado.partida;
    $('texto-ganadores').textContent = p.ganadores.length === 1
        ? `Gana ${p.ganadores[0]}`
        : `Victoria compartida: ${p.ganadores.join(', ')}`;
    const clasificacion = [...p.jugadores].sort((a, b) => b.islas - a.islas || b.barcos - a.barcos);
    $('clasificacion').replaceChildren(...clasificacion.map((j) => elemento('li', {},
        muestraColor(j.apodo),
        j.apodo,
        elemento('span', { className: 'marca', textContent: `${plural(j.islas, 'isla', 'islas')}, ${plural(j.barcos, 'barco', 'barcos')}` }),
        ...(p.ganadores.includes(j.apodo) ? [elemento('span', { className: 'marca', textContent: '🏆' })] : []))));
}

// ------------------------------------------------------------------ arranque

prepararInicio();
prepararEspera();
prepararPartida();
mostrar('inicio');
iniciarConexion();
