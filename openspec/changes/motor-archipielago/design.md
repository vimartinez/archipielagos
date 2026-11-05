# Design

## Context

El proyecto está vacío: no hay código ni build. La máquina tiene Java 21 (OpenJDK) pero no Maven ni Gradle instalados. El motor lo consumirá más adelante un servidor Spring Boot con WebSocket (cambio `salas-y-cliente-web`), que se encargará del reloj, las confirmaciones, el periodo de gracia por desconexión y la red. Ver proposal.md (Why) para la motivación y las specs para las reglas exactas.

## Goals / Non-Goals

**Goals:**
- Motor de reglas en Java puro, sin dependencias de runtime, determinista y completamente testeable con tests unitarios.
- Una API pequeña y estable que el futuro servidor pueda usar sin conocer los detalles internos de la resolución.
- Estructura de build preparada para añadir el módulo del servidor sin reorganizar nada.

**Non-Goals:**
- Concurrencia: el motor no es thread-safe; el servidor serializará el acceso a cada partida.
- Serialización JSON, persistencia o eventos en tiempo real.
- Rendimiento más allá de lo trivial (13 islas, 4 jugadores).

## Decisions

### D1. Build: Maven multimódulo con Maven Wrapper
Proyecto padre `archipielago` (groupId `com.mijuego`, `packaging pom`) con un único módulo `motor` por ahora; el servidor será otro módulo hermano. Se incluye Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) para no depender de una instalación local. Dependencias de test gestionadas desde el padre: JUnit 5 (BOM), AssertJ y Mockito.
- *Alternativa:* Gradle. Descartado porque el wrapper también requeriría una instalación inicial y Maven es lo más habitual para Spring Boot en este entorno.
- *Alternativa:* un único módulo. Descartado porque mezclaría el motor con Spring más adelante y se perdería la garantía de que el motor no depende del framework.

### D2. Paquetes
Paquete raíz `com.mijuego.archipielago.motor`:
- `mapa`: `IslaId` (enum con las 13 islas), `Ruta`, `Mapa` (consulta de rutas y distancias, salidas por número de jugadores). El mapa es una constante: no se carga de ficheros.
- `modelo`: estado inmutable (`EstadoPartida`, `EstadoIsla`, `Flota`, `Jugador`, `Orden`, `ConfiguracionPartida`).
- `resolucion`: las fases de la ronda y el informe (`InformeRonda`, `Combate`, `Captura`).
- Raíz: `Partida`, la fachada pública, y `ReglaInfringidaException`.

### D3. Estado inmutable con records y resolución como función pura
El estado de una partida es un `record` inmutable. `ResolutorRonda.resolver(estado, ordenes) -> ResultadoRonda(nuevoEstado, informe)` no tiene efectos secundarios ni aleatoriedad, y se itera siempre en orden determinista (`IslaId` por ordinal, jugadores por orden de unión), así que el determinismo de la spec se cumple por construcción y los tests no necesitan mocks. Cada fase es una función privada o un pequeño componente que transforma un estado intermedio de la ronda, en el mismo orden que la spec (salida, avance, llegada, combate, captura, producción, fin).
- *Alternativa:* entidades mutables con métodos `mover()` o `atacar()`. Descartada porque reparte la lógica de la ronda en muchas clases y hace frágil el orden de las fases.

### D4. `Partida` como fachada con estado
`Partida` guarda el `EstadoPartida` actual y las órdenes pendientes de cada jugador, y ofrece:
- `Partida.crear(List<String> apodos, Integer limiteRondas)`: valida y crea la partida (null usa el límite por defecto de 25).
- `enviarOrdenes(String jugador, List<Orden> ordenes)`: valida contra el estado al inicio de la ronda y sustituye las órdenes previas.
- `resolverRonda() -> InformeRonda`.
- `eliminarPorDesconexion(String jugador)`.
- `estado() -> EstadoPartida`: una instantánea inmutable y pública.

Esta es la única API que verá el servidor.

### D5. Combate como algoritmo único
Para cada isla con llegadas: se suman las fuerzas por jugador (guarnición del dueño más las flotas que llegan) y se descartan las de 0 barcos. Con un solo jugador, se aplica refuerzo o captura. Con más de uno, se ordena por fuerza: si el máximo es único, gana con `max - segundo`; si el máximo está empatado, gana el defensor con 0 si está entre los empatados, y si no, la isla mantiene su dueño con 0. Las reglas de empate de la spec son casos particulares de "pierdes lo que tenía el segundo", y un solo algoritmo cubre todos los escenarios.

### D6. Errores
Las violaciones de reglas (configuración inválida, orden inválida, jugador eliminado, partida terminada) lanzan `ReglaInfringidaException` (unchecked) con un mensaje legible en español que identifica la orden o el valor infractor. Validar un conjunto de órdenes es todo o nada: se valida completo antes de sustituir las órdenes previas.

## Risks / Trade-offs

- [Equilibrio del mapa sin probar: las rutas de 3 rondas pueden ser poco útiles o la salida a la interior demasiado fuerte] → El mapa vive en una sola clase constante; se ajusta en un cambio posterior tras jugar partidas reales.
- [Maven Wrapper necesita descargar Maven la primera vez] → Requiere conexión en el primer build; está documentado en el README.
- [Que el servidor use `Partida` desde varios hilos] → Queda documentado en el Javadoc de `Partida` que no es thread-safe; el servidor lo resolverá en su cambio.
- [Estado inmutable que se copia en cada ronda] → Coste despreciable con 13 islas; a cambio, las instantáneas son seguras de compartir.
