# mapa-archipielago Specification

## Purpose

Define el mapa fijo y simétrico de Archipiélago: sus islas, las rutas que las conectan con su duración en rondas y qué islas de salida se usan según el número de jugadores.

## Requirements

### Requirement: Islas del mapa
El mapa SHALL tener exactamente 13 islas con estos identificadores: 4 islas de salida (`S1`, `S2`, `S3`, `S4`), 4 bahías (`B12`, `B23`, `B34`, `B41`), 4 islas interiores (`I1`, `I2`, `I3`, `I4`) y 1 isla central (`C`).

#### Scenario: Consultar las islas
- **WHEN** se consulta el mapa
- **THEN** contiene exactamente las islas S1, S2, S3, S4, B12, B23, B34, B41, I1, I2, I3, I4 y C

### Requirement: Rutas y distancias
El mapa SHALL definir exactamente estas 24 rutas bidireccionales, cada una con una distancia en rondas, y con la misma distancia en ambos sentidos. Para cada i en 1..4, con j = i+1 (y j = 1 cuando i = 4) y k = i-1 (y k = 4 cuando i = 1):
- `Si`–`Ii`: distancia 1
- `Si`–`Bij` y `Si`–`Bki`: distancia 3
- `Ii`–`Bij` y `Ii`–`Bki`: distancia 1
- `Ii`–`C`: distancia 2

No SHALL existir rutas entre islas no listadas.

#### Scenario: Ruta corta hacia la interior
- **WHEN** se consulta la ruta entre S1 e I1
- **THEN** existe y su distancia es 1

#### Scenario: Ruta larga directa a una bahía
- **WHEN** se consulta la ruta entre S2 y B12
- **THEN** existe y su distancia es 3

#### Scenario: Ruta hacia el centro
- **WHEN** se consulta la ruta entre I3 y C
- **THEN** existe y su distancia es 2

#### Scenario: Ruta en sentido contrario
- **WHEN** se consulta la ruta entre B41 e I4
- **THEN** existe y su distancia es la misma que entre I4 y B41

#### Scenario: Islas no conectadas
- **WHEN** se consulta la ruta entre S1 y C
- **THEN** no existe

#### Scenario: Recuento de rutas
- **WHEN** se cuentan las rutas del mapa
- **THEN** hay exactamente 24

### Requirement: Simetría del mapa
El mapa SHALL ser simétrico por rotación: al sustituir en todos los identificadores cada índice n por n+1 (con 4 pasando a 1) se SHALL obtener el mismo conjunto de rutas con las mismas distancias.

#### Scenario: Rotar el mapa
- **WHEN** se aplica la rotación de índices a todas las rutas del mapa
- **THEN** el conjunto resultante de rutas y distancias es idéntico al original

### Requirement: Islas de salida según el número de jugadores
La asignación de islas de salida SHALL depender del número de jugadores, en el orden en que se unieron a la partida:
- 2 jugadores: S1 y S3
- 3 jugadores: S1, S2 y S3
- 4 jugadores: S1, S2, S3 y S4

Las islas de salida no asignadas SHALL empezar como neutrales vacías.

#### Scenario: Partida de 2 jugadores
- **WHEN** se crea una partida con los jugadores Ana y Beto
- **THEN** Ana empieza en S1, Beto en S3, y S2 y S4 son neutrales vacías

#### Scenario: Partida de 3 jugadores
- **WHEN** se crea una partida con los jugadores Ana, Beto y Cris
- **THEN** Ana empieza en S1, Beto en S2, Cris en S3 y S4 es neutral vacía
