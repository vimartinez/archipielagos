# salas Specification

## Purpose

Define cómo se reúnen los jugadores antes de una partida: creación de salas con un código para compartir, unión con apodo, el anfitrión y su relevo, la configuración del límite de rondas y el inicio de la partida.

## Requirements

### Requirement: Crear una sala
Un jugador SHALL poder crear una sala indicando su apodo. El servidor SHALL generar un código de sala de 4 caracteres, formado por letras mayúsculas y dígitos sin caracteres ambiguos (`0`, `O`, `1`, `I`), único entre las salas existentes. El creador SHALL quedar dentro de la sala como su anfitrión, y la sala SHALL empezar con un límite de rondas de 25.

#### Scenario: Sala creada
- **WHEN** Ana crea una sala con el apodo "Ana"
- **THEN** recibe un código de 4 caracteres, como "K7QX", y está en la sala como anfitriona con un límite de 25 rondas

#### Scenario: Apodo inválido al crear
- **WHEN** alguien intenta crear una sala con un apodo vacío o de más de 20 caracteres
- **THEN** se rechaza indicando que el apodo debe tener entre 1 y 20 caracteres, y no se crea la sala

### Requirement: Unirse a una sala
Un jugador SHALL poder unirse a una sala en espera indicando su código y un apodo. Los espacios al principio y al final del apodo SHALL ignorarse. El apodo SHALL tener entre 1 y 20 caracteres y no coincidir con el de otro jugador de la sala, sin distinguir mayúsculas de minúsculas. El código SHALL aceptarse en minúsculas o mayúsculas. La unión SHALL rechazarse, indicando el motivo, en estos casos:
- el código no existe;
- la sala ya tiene 4 jugadores;
- la partida de esa sala ya ha empezado;
- el apodo no es válido.

#### Scenario: Unión correcta
- **WHEN** Beto se une a la sala "K7QX" con el apodo "Beto"
- **THEN** Beto está en la sala, y todos los jugadores de la sala ven la lista actualizada con Ana y Beto

#### Scenario: Código inexistente
- **WHEN** Beto intenta unirse a la sala "ZZZZ", que no existe
- **THEN** se rechaza indicando que la sala no existe

#### Scenario: Apodo repetido
- **WHEN** en la sala está "Ana" y otra persona intenta unirse como "ana"
- **THEN** se rechaza indicando que ese apodo ya está en uso en la sala

#### Scenario: Sala llena
- **WHEN** la sala tiene 4 jugadores y un quinto intenta unirse
- **THEN** se rechaza indicando que la sala está llena

#### Scenario: Partida ya empezada
- **WHEN** alguien intenta unirse a una sala cuya partida ya ha empezado
- **THEN** se rechaza indicando que la partida ya ha empezado

### Requirement: Configurar el límite de rondas
Mientras la sala está en espera, solo el anfitrión SHALL poder cambiar el límite de rondas, con un valor entre 10 y 50 ambos incluidos. Todos los jugadores de la sala SHALL ver el límite vigente.

#### Scenario: Anfitrión cambia el límite
- **WHEN** Ana, anfitriona, fija el límite en 15 rondas
- **THEN** todos los jugadores de la sala ven un límite de 15 rondas

#### Scenario: Límite fuera de rango
- **WHEN** Ana intenta fijar el límite en 60 rondas
- **THEN** se rechaza indicando que el límite debe estar entre 10 y 50, y el límite no cambia

#### Scenario: Otro jugador intenta cambiarlo
- **WHEN** Beto, que no es anfitrión, intenta cambiar el límite de rondas
- **THEN** se rechaza indicando que solo el anfitrión puede hacerlo

### Requirement: Empezar la partida
Solo el anfitrión SHALL poder empezar la partida, y solo cuando la sala tenga entre 2 y 4 jugadores. Al empezar, SHALL crearse la partida con los jugadores en orden de llegada a la sala y el límite de rondas vigente, y todos los jugadores SHALL pasar a la partida a la vez.

#### Scenario: Inicio correcto
- **WHEN** Ana, anfitriona, empieza la partida con Ana, Beto y Cris en la sala
- **THEN** los tres reciben el estado inicial de la partida con la ronda 1 en curso

#### Scenario: Un solo jugador
- **WHEN** Ana está sola en la sala e intenta empezar
- **THEN** se rechaza indicando que se necesitan al menos 2 jugadores

#### Scenario: No anfitrión intenta empezar
- **WHEN** Beto, que no es anfitrión, intenta empezar la partida
- **THEN** se rechaza indicando que solo el anfitrión puede empezarla

### Requirement: Salir de una sala en espera
Un jugador que se desconecta o abandona una sala en espera SHALL salir de ella, y los demás SHALL ver la lista actualizada. Si el que sale es el anfitrión, el anfitrión SHALL pasar al jugador que lleva más tiempo en la sala. Si la sala se queda sin jugadores, SHALL eliminarse y su código SHALL dejar de ser válido.

#### Scenario: Sale un jugador
- **WHEN** Beto cierra la pestaña en la sala de espera
- **THEN** Beto deja de figurar en la sala y los demás ven la lista sin él

#### Scenario: Sale el anfitrión
- **WHEN** en la sala están, por orden de llegada, Ana (anfitriona), Beto y Cris, y Ana se desconecta
- **THEN** Beto pasa a ser el anfitrión y todos lo ven

#### Scenario: Sala vacía
- **WHEN** el último jugador de la sala "K7QX" se desconecta
- **THEN** la sala se elimina y un intento posterior de unirse a "K7QX" se rechaza indicando que la sala no existe
