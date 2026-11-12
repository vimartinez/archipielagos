## Why

El motor de reglas ya está terminado y probado, pero solo se puede usar desde tests: nadie puede jugar todavía. Este cambio convierte Archipiélago en un producto jugable por internet. Se crean salas a las que los amigos se unen con un código, rondas con reloj y una interfaz web, todo publicado en un hosting gratuito para poder enseñarlo funcionando.

## What Changes

- Nuevo módulo `servidor` (Spring Boot), hermano de `motor`. Usa la fachada `Partida` sin modificar el motor.
- Salas de espera:
  - Se crean con un código corto para compartir, y los jugadores se unen con código y apodo (único dentro de la sala).
  - El creador es el anfitrión. Elige el límite de rondas (10-50, 25 por defecto) y empieza la partida cuando hay de 2 a 4 jugadores.
  - Si el anfitrión se va antes de empezar, el anfitrión pasa al siguiente jugador.
  - Una sala con la partida empezada no admite jugadores nuevos.
- Partida en línea:
  - Rondas de 60 segundos. Las órdenes se envían al servidor a medida que se dan, y cada jugador puede confirmar que ha terminado.
  - La ronda se resuelve al vencer el reloj o en cuanto todos los jugadores activos han confirmado.
  - Tras cada evento, el servidor envía a todos el estado visible y, al resolver, el informe de ronda.
- Desconexión sin periodo de gracia:
  - Cerrar la conexión en la sala de espera equivale a salir de la sala.
  - Durante la partida, elimina al jugador mediante el motor.
  - Si no queda ningún jugador conectado, la partida se descarta.
- Los jugadores eliminados siguen viendo la partida hasta el final, como espectadores.
- Cliente web en HTML, CSS y JavaScript sin framework, servido por el propio servidor. Incluye:
  - pantallas de inicio, sala de espera, partida y fin;
  - mapa de 13 islas con flotas en tránsito;
  - formulario de órdenes, reloj e informe de ronda.
- Despliegue en Railway como un único servicio Docker que sirve el cliente y el WebSocket desde el mismo origen, con despliegue automático en cada push a `main`.

Fuera de alcance:
- Reconexión o periodo de gracia por desconexión.
- Persistencia de partidas: el estado vive en memoria y se pierde si el servidor se reinicia.
- Cuentas de usuario, chat, IA y varias instancias del servidor.

## Capabilities

### New Capabilities
- `salas`: creación de salas con código, unión con apodo, anfitrión y su relevo, configuración del límite de rondas, inicio de la partida y salida de la sala.
- `partida-en-linea`: desarrollo de una partida a través de la red, que cubre:
  - ronda con reloj de 60 s, envío y confirmación de órdenes y resolución anticipada;
  - difusión del estado y del informe;
  - eliminación por desconexión, espectadores y fin de partida.
- `cliente-web`: lo que el jugador ve y puede hacer en el navegador: pantallas, mapa, flotas, órdenes, reloj, informe de ronda y resultado final.

### Modified Capabilities
<!-- Ninguna: las reglas del juego (mapa, ciclo de partida y resolución de ronda) no cambian. La eliminación por desconexión ya existe en `ciclo-de-partida`; este cambio solo decide cuándo se invoca. -->

## Impact

- **Código nuevo:** módulo Maven `servidor` (Spring Boot con WebSocket), que depende de `motor`. El cliente vive en los recursos estáticos del servidor, sin build de Node.
- **Motor:** sin cambios en su API ni en sus reglas. El servidor serializa el acceso a cada `Partida`, porque no es thread-safe.
- **Despliegue:** se añade un `Dockerfile`. Railway lo detecta al conectar el repo de GitHub, así que no hace falta ningún archivo de configuración propio del host.
- **Limitaciones aceptadas:**
  - Las partidas en curso se pierden si el servicio se reinicia o se vuelve a desplegar.
  - El servicio funciona con una sola réplica.
- **README:** instrucciones para ejecutar el servidor en local y para desplegarlo.
