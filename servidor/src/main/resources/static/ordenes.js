// Composición de las órdenes de la ronda: lista local que se envía entera al servidor en cada cambio.

import { vecinas } from './mapa.js';

export class Ordenes {
    /**
     * @param {{enviar: Function, alCambiar: Function}} opciones
     *        enviar(lista) manda el conjunto al servidor; alCambiar() pide redibujar.
     */
    constructor({ enviar, alCambiar }) {
        this.enviar = enviar;
        this.alCambiar = alCambiar;
        this.lista = [];
        this.aceptadas = [];
        this.ronda = null;
    }

    /** Al empezar una ronda nueva, el servidor ya no tiene órdenes: se vacía la lista. */
    nuevaRonda(ronda) {
        if (ronda !== this.ronda) {
            this.ronda = ronda;
            this.lista = [];
            this.aceptadas = [];
        }
    }

    /** Barcos que quedan por asignar en una isla propia. */
    disponibles(partida, isla) {
        const datos = partida.islas.find((i) => i.id === isla);
        const comprometidos = this.lista.filter((o) => o.origen === isla).reduce((s, o) => s + o.barcos, 0);
        return Math.max(0, (datos?.barcos ?? 0) - comprometidos);
    }

    /** Islas propias desde las que todavía se pueden enviar barcos. */
    origenesPosibles(partida) {
        return partida.islas
            .filter((i) => i.dueno === partida.tuApodo && this.disponibles(partida, i.id) > 0)
            .map((i) => i.id);
    }

    destinosPosibles(mapa, origen) {
        return origen ? vecinas(mapa, origen) : [];
    }

    anadir(orden) {
        // Varias órdenes iguales en origen y destino se agrupan en una.
        const existente = this.lista.find((o) => o.origen === orden.origen && o.destino === orden.destino);
        if (existente) existente.barcos += orden.barcos;
        else this.lista.push({ ...orden });
        this.enviar(this.lista);
        this.alCambiar();
    }

    quitar(indice) {
        this.lista.splice(indice, 1);
        this.enviar(this.lista);
        this.alCambiar();
    }

    /** El servidor aceptó el conjunto: pasa a ser el vigente. */
    aceptar(ordenes) {
        this.aceptadas = ordenes.map((o) => ({ ...o }));
        this.lista = ordenes.map((o) => ({ ...o }));
        this.alCambiar();
    }

    /** El servidor rechazó el conjunto: se vuelve al último aceptado. */
    rechazar() {
        this.lista = this.aceptadas.map((o) => ({ ...o }));
        this.alCambiar();
    }
}
