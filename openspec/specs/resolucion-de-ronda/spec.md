# resolucion-de-ronda Specification

## Purpose

Define cómo se dan las órdenes de una ronda y cómo se resuelve de forma determinista: movimiento de flotas, combate, captura, producción e informe de lo ocurrido.

## Requirements

### Requirement: Envío de órdenes
Durante una ronda en curso, cada jugador activo SHALL poder enviar un conjunto de órdenes. Cada orden indica una isla de origen, una isla de destino y un número de barcos. Un nuevo envío del mismo jugador en la misma ronda SHALL sustituir por completo al anterior. Un conjunto vacío SHALL ser válido y significa no mover nada. Un jugador que no envía órdenes no mueve barcos en esa ronda.

#### Scenario: Sustituir las órdenes
- **WHEN** Ana envía la orden "3 barcos de S1 a I1" y después, en la misma ronda, envía "2 barcos de S1 a B12"
- **THEN** al resolver la ronda solo se ejecuta la orden "2 barcos de S1 a B12"

#### Scenario: Jugador sin órdenes
- **WHEN** Beto no envía órdenes durante la ronda
- **THEN** al resolverla ninguna flota sale de las islas de Beto

### Requirement: Validación de órdenes
Un conjunto de órdenes SHALL rechazarse completo, sin aplicarse ninguna, si alguna orden no cumple estas condiciones, indicando la orden y el motivo:
- la isla de origen está controlada por el jugador al inicio de la ronda;
- existe una ruta directa entre origen y destino;
- el número de barcos es un entero mayor o igual que 1;
- la suma de barcos que salen de cada isla de origen no supera los barcos de esa isla al inicio de la ronda.

#### Scenario: Origen ajeno
- **WHEN** Ana envía una orden con origen S3, que es de Beto
- **THEN** el conjunto se rechaza indicando que S3 no es de Ana

#### Scenario: Sin ruta directa
- **WHEN** Ana envía una orden de S1 a C
- **THEN** el conjunto se rechaza indicando que no hay ruta entre S1 y C

#### Scenario: Cero barcos
- **WHEN** Ana envía una orden de 0 barcos
- **THEN** el conjunto se rechaza indicando que hay que enviar al menos 1 barco

#### Scenario: Más barcos de los disponibles
- **WHEN** S1 tiene 5 barcos y Ana envía "3 de S1 a I1" y "3 de S1 a B12"
- **THEN** el conjunto se rechaza indicando que S1 solo tiene 5 barcos

#### Scenario: Vaciar una isla
- **WHEN** S1 tiene 5 barcos y Ana envía "5 de S1 a I1"
- **THEN** el conjunto se acepta, y tras la salida S1 sigue siendo de Ana con 0 barcos

### Requirement: Orden de las fases de resolución
La resolución de una ronda SHALL ejecutar, en este orden y sin aleatoriedad: (1) salida, (2) avance, (3) llegada, (4) combate, (5) captura, (6) producción, (7) comprobación de eliminaciones y de fin de partida. Si la partida sigue en curso, la ronda actual SHALL aumentar en 1 y las órdenes SHALL vaciarse. Resolver dos veces el mismo estado con las mismas órdenes SHALL producir siempre el mismo resultado.

#### Scenario: Resolución determinista
- **WHEN** se resuelve la misma ronda dos veces partiendo del mismo estado y con las mismas órdenes
- **THEN** ambos resultados son idénticos

#### Scenario: Avance de ronda
- **WHEN** se resuelve la ronda 4 y la partida no termina
- **THEN** la ronda actual pasa a ser la 5 y ningún jugador tiene órdenes pendientes

### Requirement: Salida, avance y llegada de flotas
En la fase de salida, cada orden SHALL restar sus barcos de la isla de origen y crear una flota con el dueño, los barcos, el origen, el destino y tantas rondas restantes como la distancia de la ruta. En la fase de avance, todas las flotas en tránsito, incluidas las recién creadas, SHALL reducir en 1 sus rondas restantes. En la fase de llegada, las flotas con 0 rondas restantes SHALL llegar a su destino. Las flotas no SHALL poder desviarse ni cancelarse una vez creadas.

#### Scenario: Ruta de distancia 1
- **WHEN** en la ronda 2 Ana ordena "4 de S1 a I1"
- **THEN** la flota llega a I1 en la resolución de la ronda 2

#### Scenario: Ruta de distancia 3
- **WHEN** en la ronda 3 Ana ordena "4 de S1 a B12"
- **THEN** tras resolver la ronda 3 la flota está en tránsito con 2 rondas restantes, tras la ronda 4 le queda 1, y llega a B12 en la resolución de la ronda 5

### Requirement: Flotas que se cruzan
Las flotas que recorren la misma ruta en sentidos opuestos SHALL cruzarse sin combatir. Las flotas solo SHALL combatir al llegar a una isla.

#### Scenario: Cruce en la misma ruta
- **WHEN** Ana tiene una flota de 4 barcos de S1 a B12 y Beto una de 2 barcos de B12 a S1 en tránsito a la vez
- **THEN** ninguna de las dos pierde barcos durante el trayecto y cada una llega a su destino

### Requirement: Combate en una isla
En cada isla donde, tras la llegada, haya fuerzas (al menos 1 barco) de más de un jugador, SHALL producirse un combate. Las fuerzas de cada jugador SHALL ser la suma de sus barcos en la isla y los barcos de sus flotas que acaban de llegar; el defensor es el dueño de la isla. Si un único jugador tiene la mayor fuerza, SHALL ganar y conservar su fuerza menos la segunda mayor fuerza. Si varios empatan con la mayor fuerza y el defensor está entre ellos, SHALL ganar el defensor con 0 barcos. Si varios empatan con la mayor fuerza y el defensor no está entre ellos, no hay ganador: la isla SHALL conservar su dueño (o seguir neutral) con 0 barcos. Todos los jugadores que no ganen SHALL perder todos los barcos implicados. Si solo un jugador tiene fuerzas en la isla, no hay combate y sus barcos se suman.

#### Scenario: Gana el mayor y pierde lo del segundo
- **WHEN** I2 es de Cris con 2 barcos y llegan flotas de Ana con 5 y de Beto con 4
- **THEN** gana Ana, que se queda I2 con 1 barco, y Beto y Cris pierden sus barcos

#### Scenario: Empate con el defensor
- **WHEN** I1 es de Ana con 3 barcos y llega una flota de Beto con 3
- **THEN** Ana conserva I1 con 0 barcos y Beto pierde su flota

#### Scenario: Empate entre atacantes
- **WHEN** I2 es de Cris con 2 barcos y llegan flotas de Ana con 5 y de Beto con 5
- **THEN** I2 sigue siendo de Cris con 0 barcos y Ana y Beto pierden sus flotas

#### Scenario: Empate en una isla neutral
- **WHEN** C es neutral con 0 barcos y llegan flotas de Ana con 4 y de Beto con 4
- **THEN** C sigue neutral con 0 barcos y ambas flotas se pierden

#### Scenario: Refuerzo del defensor
- **WHEN** I1 es de Ana con 2 barcos, llega una flota de Ana con 3 y una de Beto con 4
- **THEN** la fuerza de Ana es 5, gana Ana y se queda I1 con 1 barco

#### Scenario: Refuerzo sin enemigos
- **WHEN** I1 es de Ana con 2 barcos y solo llega una flota de Ana con 3
- **THEN** no hay combate e I1 queda con 5 barcos de Ana

### Requirement: Captura de islas
Si gana el combate un jugador distinto del dueño, o si llegan fuerzas de un solo jugador a una isla neutral o ajena sin barcos, ese jugador SHALL pasar a controlar la isla con los barcos que le queden.

#### Scenario: Captura de una isla neutral
- **WHEN** C es neutral con 0 barcos y solo llega una flota de Ana con 3
- **THEN** C pasa a ser de Ana con 3 barcos

#### Scenario: Captura de una isla enemiga vacía
- **WHEN** B23 es de Beto con 0 barcos y llega una flota de Ana con 2
- **THEN** B23 pasa a ser de Ana con 2 barcos

### Requirement: Producción
Tras las capturas, cada isla controlada por un jugador activo SHALL ganar 1 barco, incluidas las capturadas en esa misma ronda y las que tengan 0 barcos. Las islas neutrales SHALL no producir.

#### Scenario: Producción tras capturar
- **WHEN** en una ronda Ana conserva S1 con 2 barcos y captura C quedándose con 3
- **THEN** al terminar la ronda S1 tiene 3 barcos y C tiene 4

#### Scenario: Neutrales sin producción
- **WHEN** termina una ronda en la que B34 es neutral con 0 barcos
- **THEN** B34 sigue con 0 barcos

### Requirement: Informe de ronda
La resolución de cada ronda SHALL devolver un informe con la ronda resuelta y, por cada combate, la isla, las fuerzas de cada jugador implicado, el ganador (o la ausencia de ganador) y los barcos que le quedan; además, las islas que cambiaron de dueño, los jugadores eliminados en esa ronda y, si la partida terminó, los ganadores.

#### Scenario: Informe de una ronda con combate
- **WHEN** en la ronda 7 Ana gana un combate en I2 con fuerzas Ana 5, Beto 4 y Cris 2
- **THEN** el informe de la ronda 7 incluye ese combate con las fuerzas de los tres, Ana como ganadora con 1 barco, y la captura de I2 por Ana

#### Scenario: Informe de una ronda tranquila
- **WHEN** se resuelve una ronda sin combates, capturas ni eliminaciones
- **THEN** el informe indica la ronda resuelta y listas vacías de combates, capturas y eliminaciones
