## Purpose

Define cómo transcurre una partida a través de la red: el reloj de cada ronda, el envío y la confirmación de órdenes, la resolución anticipada, la difusión del estado y del informe, la eliminación por desconexión, los espectadores y el fin de la partida. Las reglas del juego son las de `ciclo-de-partida` y `resolucion-de-ronda`.

## ADDED Requirements

### Requirement: Reloj de la ronda
Cada ronda SHALL tener un tiempo límite de 60 segundos, que empieza cuando la ronda empieza. El servidor SHALL comunicar a todos los jugadores el instante en que vence el tiempo de la ronda en curso. Al vencer el tiempo, la ronda SHALL resolverse con las órdenes vigentes de cada jugador, haya confirmado o no.

#### Scenario: Vence el tiempo
- **WHEN** pasan 60 segundos desde el inicio de la ronda 3 y Beto no ha confirmado, aunque tiene enviada la orden "2 de S2 a B23"
- **THEN** la ronda 3 se resuelve ejecutando la orden de Beto, y empieza la ronda 4 con un nuevo plazo de 60 segundos

#### Scenario: Jugador sin órdenes al vencer el tiempo
- **WHEN** vence el tiempo y Cris no ha enviado ninguna orden en esa ronda
- **THEN** la ronda se resuelve sin mover barcos de Cris

### Requirement: Envío de órdenes en línea
Durante la ronda, cada jugador activo que no haya confirmado SHALL poder enviar su conjunto de órdenes tantas veces como quiera, y cada envío SHALL sustituir al anterior según las reglas de `resolucion-de-ronda`. Si el conjunto es inválido, SHALL rechazarse solo al jugador que lo envió, indicando el motivo, y sus órdenes vigentes SHALL seguir siendo las anteriores. Las órdenes de un jugador SHALL ser secretas: el servidor no SHALL enviarlas a los demás jugadores.

#### Scenario: Órdenes aceptadas
- **WHEN** Ana envía "3 de S1 a I1"
- **THEN** Ana recibe la confirmación de que sus órdenes vigentes son "3 de S1 a I1", y los demás no reciben esas órdenes

#### Scenario: Órdenes rechazadas
- **WHEN** Ana tiene vigente "3 de S1 a I1" y envía "9 de S1 a I1" teniendo S1 5 barcos
- **THEN** Ana recibe un error indicando que S1 solo tiene 5 barcos, y su orden vigente sigue siendo "3 de S1 a I1"

### Requirement: Confirmación de órdenes
Cada jugador activo SHALL poder confirmar que ha terminado de dar órdenes en la ronda en curso, con o sin órdenes enviadas. Una vez confirmado, el jugador no SHALL poder cambiar sus órdenes en esa ronda. Todos los jugadores SHALL ver quién ha confirmado, pero no sus órdenes.

#### Scenario: Confirmar sin órdenes
- **WHEN** Beto confirma sin haber enviado órdenes
- **THEN** Beto figura como confirmado para todos y no mueve barcos en esa ronda

#### Scenario: Cambiar órdenes tras confirmar
- **WHEN** Ana ha confirmado e intenta enviar nuevas órdenes en la misma ronda
- **THEN** se rechaza indicando que ya confirmó sus órdenes para esta ronda

### Requirement: Resolución anticipada
Si todos los jugadores activos han confirmado antes de que venza el tiempo, la ronda SHALL resolverse en ese momento, sin esperar al final del plazo.

#### Scenario: Todos confirman
- **WHEN** a los 20 segundos de la ronda 5 confirma el último de los jugadores activos
- **THEN** la ronda 5 se resuelve inmediatamente y empieza la ronda 6 con un plazo de 60 segundos

#### Scenario: Falta uno por confirmar
- **WHEN** Ana y Beto han confirmado y Cris, también activo, no
- **THEN** la ronda sigue en curso hasta que Cris confirme o venza el tiempo

### Requirement: Difusión del estado y del informe
Tras cada cambio relevante, el servidor SHALL enviar a todos los conectados a la partida el estado público de la partida (según `ciclo-de-partida`). Esto incluye el inicio de la partida, cada ronda resuelta, cada confirmación y cada eliminación por desconexión. Junto al estado SHALL enviar la ronda en curso, el instante en que vence su plazo y quién ha confirmado. Al resolver una ronda, SHALL enviar además el informe de esa ronda.

#### Scenario: Ronda resuelta
- **WHEN** se resuelve la ronda 7, en la que hubo un combate en I2
- **THEN** todos los conectados reciben el informe de la ronda 7 con ese combate y el estado de la ronda 8

### Requirement: Eliminación por desconexión durante la partida
Si un jugador activo se desconecta durante la partida, el servidor SHALL eliminarlo inmediatamente según la regla de eliminación por desconexión de `ciclo-de-partida`, sin periodo de gracia ni posibilidad de volver a esa partida. Si tras la eliminación todos los jugadores activos restantes ya han confirmado, la ronda SHALL resolverse en ese momento. Si la eliminación deja un solo jugador activo, la partida SHALL terminar con ese jugador como ganador.

#### Scenario: Un jugador cierra la pestaña
- **WHEN** en una partida de 3 jugadores Cris cierra la pestaña
- **THEN** Cris queda eliminado, sus islas pasan a neutrales y sus flotas desaparecen, y Ana y Beto ven el estado actualizado

#### Scenario: El que faltaba por confirmar se desconecta
- **WHEN** Ana y Beto han confirmado y Cris, el único que faltaba, se desconecta
- **THEN** Cris queda eliminado y la ronda se resuelve inmediatamente

#### Scenario: Queda un solo jugador
- **WHEN** en una partida de 2 jugadores Beto se desconecta
- **THEN** la partida termina y Ana ve que es la ganadora

### Requirement: Espectadores
Un jugador eliminado por quedarse sin fuerzas que sigue conectado SHALL seguir recibiendo el estado y los informes de la partida hasta el final, sin poder enviar ni confirmar órdenes. Su desconexión posterior no SHALL afectar a la partida.

#### Scenario: Eliminado que sigue mirando
- **WHEN** Beto pierde su última isla y su última flota en la ronda 9 y sigue conectado
- **THEN** Beto recibe el estado y los informes de las rondas siguientes, y si intenta enviar órdenes se rechaza indicando que está eliminado

#### Scenario: Ronda sin esperar a los eliminados
- **WHEN** Beto está eliminado y Ana y Cris, los activos, han confirmado
- **THEN** la ronda se resuelve sin esperar a Beto

### Requirement: Fin de la partida en línea
Cuando la partida termina, el servidor SHALL enviar a todos los conectados el estado final, el informe de la última ronda si la hubo, y los ganadores. Después no SHALL aceptar órdenes ni confirmaciones, y el reloj SHALL detenerse. La sala y su partida SHALL eliminarse cuando se desconecte el último conectado. Si todos se desconectan antes de que la partida termine, la partida SHALL descartarse y la sala SHALL eliminarse.

#### Scenario: Fin por límite de rondas
- **WHEN** se resuelve la última ronda permitida
- **THEN** todos los conectados reciben el informe, el estado final y los ganadores, y no empieza ninguna ronda nueva

#### Scenario: Todos se van
- **WHEN** todos los jugadores de una partida en curso se desconectan
- **THEN** la partida se descarta, su reloj se detiene y su código de sala deja de ser válido
