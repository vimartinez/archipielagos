# Proposal

## Why

Archipiélago es un juego web de estrategia multijugador por turnos simultáneos. Antes de construir salas, red e interfaz necesitamos un motor de reglas fiable y determinista que decida qué ocurre en cada ronda. Aislarlo primero permite validar y equilibrar las reglas con tests, sin depender de servidor ni navegador, y deja una base estable para el cambio posterior (`salas-y-cliente-web`).

## What Changes

- Nuevo proyecto Java con un módulo de motor de juego independiente de cualquier framework.
- Mapa fijo y simétrico de 13 islas con rutas de distintas longitudes (1 a 3 rondas de trayecto).
- Creación de partidas para 2-4 jugadores con límite de rondas configurable (10-50, 25 por defecto); cada jugador empieza con 1 isla y 5 barcos, y las demás islas son neutrales y vacías.
- Órdenes secretas por ronda: enviar N barcos de una isla propia a una isla conectada.
- Resolución determinista de cada ronda en fases fijas: salida, avance, llegada, combate, captura, producción y fin de partida.
  - Flotas en tránsito durante varias rondas, visibles para todos, sin desvío posible; las flotas que se cruzan no combaten.
  - Combate: gana el mayor y pierde tantos barcos como tenía el segundo; ante un empate en cabeza gana el defensor si está entre los empatados, y si no hay ganador único la isla no cambia de dueño.
  - Producción de +1 barco por isla controlada.
- Eliminación de jugadores (sin islas ni flotas, o por desconexión notificada desde fuera del motor): sus islas pasan a neutrales vacías y sus flotas desaparecen.
- Fin de partida: gana el último jugador en pie o, al alcanzar el límite de rondas, quien tenga más islas, desempatando por barcos totales; si persiste el empate, la victoria es compartida.
- Informe de ronda con los combates, capturas y eliminaciones ocurridos, para que la futura interfaz pueda mostrarlos.

Fuera de alcance: salas, códigos de partida, reloj de 60 s, confirmaciones, periodo de gracia por desconexión, red e interfaz (cambio `salas-y-cliente-web`); cuentas, mapas generados, IA, niebla de guerra.

## Capabilities

### New Capabilities
- `mapa-archipielago`: el mapa fijo de 13 islas, sus rutas, distancias y las islas de salida según el número de jugadores.
- `ciclo-de-partida`: creación y configuración de la partida, estado inicial, eliminación de jugadores y condiciones de fin y victoria.
- `resolucion-de-ronda`: validación de órdenes, movimiento de flotas, combate, captura, producción e informe de ronda.

### Modified Capabilities
<!-- Ninguna: es el primer cambio del proyecto. -->

## Impact

- Código nuevo: proyecto Maven (Java 21) con un módulo `motor` sin dependencias de runtime; JUnit 5, Mockito y AssertJ solo para tests.
- Sin APIs externas, red ni persistencia en este cambio: el estado vive en memoria y lo consumirá el futuro servidor.
- Herramientas: la máquina no tiene Maven instalado, así que el proyecto incluirá Maven Wrapper.
