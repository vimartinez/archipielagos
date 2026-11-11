# Spec Delta

## Purpose

Gobierna el ciclo de vida de una partida de Archipiélago: su creación y configuración, el estado inicial, la eliminación de jugadores y las condiciones de fin y victoria.

## ADDED Requirements

### Requirement: Creación de partida
El motor SHALL crear una partida a partir de una lista ordenada de 2 a 4 jugadores, cada uno con un apodo no vacío y distinto de los demás, y de un límite de rondas entre 10 y 50 (ambos incluidos). Si no se indica el límite, SHALL usar 25. Cualquier otra configuración SHALL rechazarse con un error que indique el motivo, sin crear la partida.

#### Scenario: Partida válida con límite por defecto
- **WHEN** se crea una partida con los jugadores Ana y Beto sin indicar límite de rondas
- **THEN** la partida se crea con un límite de 25 rondas

#### Scenario: Límite de rondas configurado
- **WHEN** se crea una partida con 3 jugadores y un límite de 40 rondas
- **THEN** la partida se crea con un límite de 40 rondas

#### Scenario: Demasiados pocos jugadores
- **WHEN** se intenta crear una partida con un solo jugador
- **THEN** se rechaza indicando que se necesitan entre 2 y 4 jugadores

#### Scenario: Demasiados jugadores
- **WHEN** se intenta crear una partida con 5 jugadores
- **THEN** se rechaza indicando que se necesitan entre 2 y 4 jugadores

#### Scenario: Límite de rondas fuera de rango
- **WHEN** se intenta crear una partida con un límite de 9 o de 51 rondas
- **THEN** se rechaza indicando que el límite debe estar entre 10 y 50

#### Scenario: Apodos repetidos o vacíos
- **WHEN** se intenta crear una partida en la que dos jugadores tienen el mismo apodo, o alguno tiene un apodo vacío
- **THEN** se rechaza indicando qué apodo no es válido

### Requirement: Estado inicial
Al crearse, la partida SHALL estar en la ronda 1, en curso. Cada jugador SHALL controlar únicamente su isla de salida con 5 barcos. Todas las demás islas SHALL ser neutrales con 0 barcos y no SHALL haber flotas en tránsito.

#### Scenario: Estado al empezar
- **WHEN** se crea una partida con Ana y Beto
- **THEN** la ronda actual es 1, S1 es de Ana con 5 barcos, S3 es de Beto con 5 barcos, las otras 11 islas son neutrales con 0 barcos y no hay flotas

### Requirement: Estado público de la partida
El motor SHALL exponer el estado completo de la partida, visible para todos los jugadores: ronda actual, límite de rondas, si sigue en curso, dueño y barcos de cada isla, y cada flota en tránsito con su dueño, número de barcos, origen, destino y rondas que le faltan para llegar. También SHALL indicar qué jugadores siguen activos y cuáles están eliminados.

#### Scenario: Flotas enemigas visibles
- **WHEN** Beto tiene una flota de 3 barcos en tránsito de S3 a B34 a la que le faltan 2 rondas
- **THEN** el estado de la partida muestra esa flota con dueño Beto, 3 barcos, origen S3, destino B34 y 2 rondas restantes

### Requirement: Eliminación por quedarse sin fuerzas
Al final de la resolución de una ronda, un jugador activo que no controle ninguna isla y no tenga flotas en tránsito SHALL quedar eliminado.

#### Scenario: Jugador sin islas ni flotas
- **WHEN** en una ronda Beto pierde su última isla y no tiene flotas en tránsito
- **THEN** al terminar la resolución Beto queda eliminado

#### Scenario: Jugador sin islas pero con flotas
- **WHEN** en una ronda Beto pierde su última isla pero tiene una flota en tránsito
- **THEN** Beto sigue activo

### Requirement: Eliminación por desconexión
El motor SHALL permitir eliminar a un jugador activo por desconexión en cualquier momento mientras la partida esté en curso. El jugador eliminado dejará de poder dar órdenes, sus órdenes pendientes de la ronda actual SHALL descartarse, sus islas SHALL pasar a neutrales con 0 barcos y sus flotas en tránsito SHALL desaparecer. Si tras la eliminación solo queda un jugador activo, la partida SHALL terminar de inmediato con ese jugador como ganador.

#### Scenario: Desconexión con islas y flotas
- **WHEN** se elimina por desconexión a Cris, que controla S2 e I2 y tiene una flota en tránsito
- **THEN** S2 e I2 pasan a ser neutrales con 0 barcos, la flota de Cris desaparece y Cris figura como eliminado

#### Scenario: Desconexión que deja un solo jugador
- **WHEN** en una partida de 2 jugadores se elimina por desconexión a Beto
- **THEN** la partida termina y Ana es la ganadora

#### Scenario: Órdenes de un jugador eliminado
- **WHEN** un jugador eliminado intenta enviar órdenes
- **THEN** se rechazan indicando que el jugador está eliminado

### Requirement: Fin por último jugador en pie
Si al terminar la resolución de una ronda queda exactamente un jugador activo, la partida SHALL terminar y ese jugador SHALL ser el ganador, aunque no se haya alcanzado el límite de rondas. Si en la misma ronda quedan eliminados todos los jugadores activos restantes, la partida SHALL terminar en empate entre todos ellos.

#### Scenario: Victoria anticipada
- **WHEN** en la ronda 12 de una partida con límite de 25 queda eliminado el último rival de Ana
- **THEN** la partida termina en la ronda 12 y Ana es la ganadora

#### Scenario: Eliminación simultánea de todos
- **WHEN** Ana y Beto son los únicos activos y ambos quedan eliminados en la misma ronda
- **THEN** la partida termina en empate entre Ana y Beto

### Requirement: Fin por límite de rondas
Si la partida sigue en curso después de resolver la ronda igual al límite, SHALL terminar. Ganará el jugador activo con más islas controladas; si hay empate, entre los empatados el que tenga más barcos totales (en sus islas más los de sus flotas en tránsito); si sigue habiendo empate, todos los empatados SHALL compartir la victoria. Los jugadores eliminados no pueden ganar.

#### Scenario: Gana quien tiene más islas
- **WHEN** termina la última ronda con Ana controlando 6 islas y Beto 4
- **THEN** la partida termina y Ana es la ganadora

#### Scenario: Desempate por barcos
- **WHEN** termina la última ronda con Ana y Beto controlando 5 islas cada uno, Ana con 20 barcos totales y Beto con 17
- **THEN** Ana es la ganadora

#### Scenario: Victoria compartida
- **WHEN** termina la última ronda con Ana y Beto empatados en islas y en barcos totales
- **THEN** Ana y Beto comparten la victoria

### Requirement: Partida terminada
Una vez terminada, la partida SHALL rechazar nuevas órdenes y nuevas resoluciones de ronda, indicando que la partida ha terminado.

#### Scenario: Órdenes tras el final
- **WHEN** se envían órdenes o se intenta resolver una ronda en una partida terminada
- **THEN** se rechaza indicando que la partida ha terminado
