# cliente-web Specification

## Purpose

Define lo que el jugador ve y puede hacer en el navegador para jugar a Archipiélago: entrar en una sala, esperar a los demás, dar órdenes sobre el mapa con el reloj a la vista, entender lo que pasó en cada ronda y conocer el resultado final.

## Requirements

### Requirement: Pantalla de inicio
El cliente SHALL ofrecer una pantalla de inicio donde el jugador escribe su apodo y elige entre crear una sala o unirse a una con su código. Los errores del servidor, como un código inexistente o un apodo en uso, SHALL mostrarse en esa pantalla sin perder lo escrito.

#### Scenario: Unirse con código
- **WHEN** el jugador escribe "Beto" y el código "k7qx" y pulsa "Unirse"
- **THEN** pasa a la sala de espera de la sala "K7QX"

#### Scenario: Error al unirse
- **WHEN** el jugador intenta unirse a una sala que no existe
- **THEN** ve el mensaje "La sala no existe" y su apodo sigue escrito

### Requirement: Conexión con el servidor
Mientras el cliente establece la conexión con el servidor, SHALL indicarlo al jugador. Si la conexión tarda más de 3 segundos, SHALL indicar que el servidor puede estar despertando y que la espera puede durar alrededor de un minuto. Si la conexión se pierde, SHALL informar al jugador de que ha quedado fuera de la partida y ofrecer volver a la pantalla de inicio.

#### Scenario: Servidor dormido
- **WHEN** el jugador abre el juego y el servidor tarda 30 segundos en responder
- **THEN** ve un aviso de que el servidor está despertando, en lugar de un error

#### Scenario: Conexión perdida
- **WHEN** se pierde la conexión durante la partida
- **THEN** el jugador ve que ha quedado fuera de la partida y un botón para volver al inicio

### Requirement: Sala de espera
La sala de espera SHALL mostrar:
- el código de la sala, con una forma de copiarlo;
- la lista de jugadores en orden de llegada, con el anfitrión marcado;
- el límite de rondas.

Solo el anfitrión SHALL ver controles para cambiar el límite de rondas y para empezar la partida. El botón de empezar SHALL estar desactivado mientras haya menos de 2 jugadores.

#### Scenario: Vista del anfitrión
- **WHEN** Ana, anfitriona, está en la sala con Beto
- **THEN** ve el código, los dos jugadores con Ana marcada como anfitriona, el selector de límite de rondas y el botón "Empezar" activo

#### Scenario: Vista de un invitado
- **WHEN** Beto está en la sala de Ana
- **THEN** ve el código, la lista de jugadores y el límite de rondas, pero no los controles de configurar ni de empezar

### Requirement: Mapa de la partida
La pantalla de partida SHALL dibujar el mapa con sus 13 islas en posiciones fijas y las rutas entre ellas, indicando la duración en rondas de cada ruta. Cada isla SHALL mostrar su nombre, su dueño (con un color por jugador; las neutrales, en gris) y sus barcos. Cada flota en tránsito SHALL mostrarse sobre su ruta con el color de su dueño, sus barcos, y las rondas que le faltan para llegar.

#### Scenario: Flota en tránsito
- **WHEN** Beto tiene una flota de 3 barcos de S3 a B34 a la que le faltan 2 rondas
- **THEN** el mapa muestra sobre la ruta S3-B34 una flota del color de Beto con "3" barcos y "2" rondas restantes

### Requirement: Dar y confirmar órdenes
Un jugador activo SHALL poder componer órdenes eligiendo una isla propia de origen, una isla destino con ruta directa desde ella y un número de barcos. Solo SHALL poder elegir como destino las islas conectadas con el origen. SHALL ver la lista de órdenes de la ronda y los barcos que quedan en cada isla de origen, y SHALL poder quitar órdenes de la lista. Cada cambio en la lista SHALL enviarse al servidor como el nuevo conjunto de órdenes. Los errores del servidor SHALL mostrarse junto a la lista. Un botón "Confirmar" SHALL confirmar las órdenes de la ronda, y tras confirmar SHALL quedar bloqueada la edición hasta la ronda siguiente.

#### Scenario: Componer una orden
- **WHEN** Ana elige S1 como origen
- **THEN** solo puede elegir como destino las islas conectadas con S1, y un número de barcos entre 1 y los que quedan disponibles en S1

#### Scenario: Confirmar
- **WHEN** Ana pulsa "Confirmar"
- **THEN** no puede editar sus órdenes hasta la siguiente ronda y ve que está esperando a los demás

### Requirement: Reloj y confirmaciones visibles
Durante la ronda, el cliente SHALL mostrar el número de ronda, el límite de rondas, una cuenta atrás de los segundos que quedan según el plazo indicado por el servidor, y qué jugadores han confirmado.

#### Scenario: Cuenta atrás
- **WHEN** quedan 12 segundos para que venza la ronda 4 de 25 y Beto ha confirmado
- **THEN** el jugador ve "Ronda 4 / 25", una cuenta atrás en 12, y a Beto marcado como confirmado

### Requirement: Informe de ronda visible
Al resolverse una ronda, el cliente SHALL mostrar su informe de forma legible:
- cada combate, con su isla, las fuerzas de cada jugador, el ganador y los barcos que le quedan;
- las islas capturadas;
- los jugadores eliminados.

El informe SHALL seguir consultable durante la ronda siguiente.

#### Scenario: Informe con combate
- **WHEN** se resuelve una ronda en la que Ana ganó en I2 con fuerzas Ana 5 y Beto 4
- **THEN** el jugador ve un resumen del tipo "I2: Ana 5 contra Beto 4, gana Ana con 1 barco" y que Ana capturó I2

### Requirement: Espectador y fin de partida
Un jugador eliminado que sigue conectado SHALL ver que está eliminado y seguir viendo el mapa, el reloj y los informes, sin controles de órdenes. Al terminar la partida, el cliente SHALL mostrar los ganadores y la clasificación final por islas y barcos, y SHALL ofrecer volver a la pantalla de inicio.

#### Scenario: Eliminado mirando
- **WHEN** Beto ha sido eliminado y sigue conectado
- **THEN** ve el aviso "Has sido eliminado", el mapa se sigue actualizando y no tiene controles de órdenes

#### Scenario: Pantalla final
- **WHEN** termina la partida con Ana como ganadora
- **THEN** todos los conectados ven a Ana como ganadora, la clasificación final y un botón para volver al inicio
