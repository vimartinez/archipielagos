## Context

El proyecto es un Maven multimódulo (Java 21, Maven Wrapper) con un único módulo, `motor`, que no tiene dependencias de runtime. Su fachada `Partida` ofrece:
- `crear`
- `estado`
- `enviarOrdenes`
- `resolverRonda`
- `eliminarPorDesconexion`

Lanza `ReglaInfringidaException` con mensajes en español. No es thread-safe, y el diseño del motor ya delegaba en el servidor la tarea de serializar el acceso. La motivación y el alcance están en proposal.md, y el comportamiento exigido, en las specs `salas`, `partida-en-linea` y `cliente-web`.

El hosting objetivo es el plan gratuito de Render: un único contenedor que puede dormirse y reiniciarse, sin disco persistente y con una sola instancia.

## Goals / Non-Goals

**Goals:**
- La lógica de salas y rondas (estados, reloj, confirmaciones, desconexiones) debe poder probarse sin red, sin Spring y sin esperas reales.
- Un único artefacto desplegable: un contenedor que sirve el cliente estático y el WebSocket desde el mismo origen.
- Un cliente sin paso de build, que se edite y se recargue directamente.
- El motor queda intacto: ningún cambio en su API ni en sus reglas.

**Non-Goals:**
- Escalar a varias instancias o sobrevivir a reinicios.
- Un diseño cuidado para móvil. El cliente se diseña para escritorio; en móvil puede funcionar, pero no se garantiza.
- Seguridad más allá de lo básico. No hay autenticación: la identidad de un jugador es su conexión.

## Decisions

### D1. Módulo `servidor` con Spring Boot y WebSocket nativo
Se añade el módulo Maven `servidor`, hermano de `motor` y dependiente de él, con Spring Boot (la versión estable vigente, 4.x, importada como BOM en el pom padre) y `spring-boot-starter-websocket`. Se usa la API de WebSocket de Spring (`WebSocketHandler`) con mensajes JSON propios en `/ws`.
- *Alternativa:* STOMP sobre WebSocket. Descartada porque añade un broker, suscripciones y una librería JS cliente, y no hay temas que suscribir: cada conexión pertenece a una sola sala.
- *Alternativa:* Javalin o Jetty directo. Descartada porque el diseño del motor ya anticipaba Spring Boot y simplifica servir los estáticos, la configuración por entorno y el empaquetado.

### D2. Núcleo de salas sin Spring, y adaptador WebSocket fino
Paquete raíz `com.mijuego.archipielago.servidor`:
- **`sala`: el núcleo, en Java puro.**
  - `GestorSalas` registra las salas por código y genera los códigos.
  - `Sala` es la máquina de estados `EN_ESPERA -> EN_PARTIDA -> TERMINADA`. Contiene los jugadores, el anfitrión, el límite, la `Partida`, las confirmaciones y la ronda en curso.
  - Las órdenes entran como comandos: `unirse`, `salir`, `configurarLimite`, `empezar`, `enviarOrdenes`, `confirmar`, `desconectar` y `plazoVencido`.
- **`sala`: dos puertos que el núcleo usa hacia fuera.**
  - `Notificador`: enviar un mensaje a un jugador o a toda la sala.
  - `Planificador`: programar o cancelar el vencimiento de la ronda. El tiempo actual se obtiene de un `java.time.Clock`.
- **`ws`:** `JuegoWebSocketHandler` traduce JSON a comandos y comandos a JSON. Mapea cada `WebSocketSession` a su sala y apodo, e implementa `Notificador`.
- **`dto`:** records de los mensajes del protocolo, junto con la conversión del estado y del informe del motor a esos DTO.

Los tests del núcleo usan un `Notificador` que graba los mensajes y un `Planificador` manual que permite "avanzar el reloj". Así cubren todos los escenarios de `salas` y `partida-en-linea` sin hilos ni esperas.
- *Alternativa:* poner la lógica en el handler WebSocket. Descartada porque obliga a probar con sockets reales y reloj real.

### D3. Concurrencia: cada sala serializa sus eventos
Todos los comandos sobre una sala se ejecutan con `synchronized` sobre esa sala: los mensajes de jugadores, los cierres de conexión y los vencimientos del reloj. Como cada `Partida` pertenece a una sola `Sala`, nunca se usa desde dos hilos a la vez. `GestorSalas` usa un `ConcurrentHashMap` para el registro.

El reloj usa un único `ScheduledExecutorService` compartido. Cada tarea programada lleva el número de ronda para el que se creó; al ejecutarse dentro del lock, si la sala ya no está en esa ronda (porque se resolvió antes por confirmaciones), no hace nada. Esto elimina las carreras entre "todos confirman" y "vence el plazo" sin tener que cancelar tareas con precisión.
- *Alternativa:* un hilo o actor por sala. Descartada porque hay pocas salas y el lock es más simple de razonar y de probar.

### D4. Protocolo JSON
Un solo endpoint, `/ws`. Cada mensaje es un objeto con un campo `tipo`.

| Dirección | `tipo` | Contenido |
|---|---|---|
| cliente -> servidor | `crearSala` | `apodo` |
| cliente -> servidor | `unirse` | `codigo`, `apodo` |
| cliente -> servidor | `configurarLimite` | `limiteRondas` |
| cliente -> servidor | `empezar` | — |
| cliente -> servidor | `ordenes` | `ordenes: [{origen, destino, barcos}]` |
| cliente -> servidor | `confirmar` | — |
| servidor -> cliente | `sala` | `codigo`, `tuApodo`, `jugadores` (en orden), `anfitrion`, `limiteRondas` |
| servidor -> cliente | `mapa` | islas y rutas con su distancia (se envía una vez, al empezar la partida) |
| servidor -> cliente | `partida` | `tuApodo`, el estado público, `ronda`, `msRestantes`, `confirmados` |
| servidor -> cliente | `informe` | el informe de la ronda resuelta |
| servidor -> cliente | `ordenesAceptadas` | las órdenes vigentes de ese jugador |
| servidor -> cliente | `error` | `mensaje` (el texto de `ReglaInfringidaException` o de la validación de sala) |

- **El estado se envía completo en cada cambio.** Con 13 islas es poco tamaño, y el cliente no tiene que conciliar diferencias.
- **El plazo se envía como `msRestantes`, no como instante absoluto.** El cliente calcula el vencimiento con su propio reloj, así que no le afecta el desfase entre relojes.
- **El servidor envía el mapa.** El cliente no duplica la tabla de rutas del motor; solo guarda las coordenadas de dibujo de cada isla.

### D5. Identidad y detección de desconexiones
La identidad de un jugador es su `WebSocketSession`: no hay tokens ni reconexión. El cierre de la sesión, voluntario o por error, invoca el comando `desconectar`.

Para detectar conexiones muertas, sin un cierre limpio (portátil suspendido, red caída), el servidor envía un ping WebSocket cada 25 s. Los navegadores responden con pong automáticamente. Si no llega ningún pong en 60 s, el servidor cierra la sesión, lo que provoca la eliminación. Cada intervalo es configurable.

### D6. Códigos de sala
4 caracteres de un alfabeto de 32: letras mayúsculas y dígitos sin `0`, `O`, `1` ni `I`, unas 10^6 combinaciones. Se generan con `SecureRandom` y se reintenta si el código coincide con una sala existente. La entrada se normaliza a mayúsculas.

### D7. Cliente estático sin build
Archivos en `servidor/src/main/resources/static/`:
- `index.html`
- `estilos.css`
- módulos ES nativos: `app.js` (máquina de pantallas inicio / espera / partida / fin), `conexion.js` (WebSocket, avisos de conexión lenta y perdida), `mapa.js` (SVG) y `ordenes.js`.

El mapa es un SVG generado desde JS: cada isla tiene una coordenada fija y simétrica guardada en el cliente, y las rutas y distancias llegan en el mensaje `mapa`. Cada flota en tránsito se dibuja sobre su ruta en la posición proporcional a su avance, que se calcula a partir de las rondas restantes y de la distancia.

La URL del WebSocket se deriva de `location`: `wss` si la página va por `https` y `ws` si no. Así funciona igual en local y en Render.
- *Alternativa:* React con Vite. Descartada porque exige Node y un paso de build en Docker y en local, a cambio de poco con 4 pantallas.

### D8. Despliegue en Render
- **`Dockerfile` multi-stage.**
  - Etapa de build: `eclipse-temurin:21-jdk` ejecuta `./mvnw -pl servidor -am package -DskipTests`. Los tests ya los pasa el desarrollador antes del push.
  - Etapa final: `eclipse-temurin:21-jre` con el jar ejecutable de Spring Boot.
- **Puerto.** `server.port=${PORT:8080}`, porque Render inyecta `PORT`.
- **`render.yaml`.** Define un servicio `web` con `runtime: docker`, `plan: free`, `autoDeploy: true` desde `main` y `healthCheckPath: /`.
- **Arranque.** Como el servicio se duerme, el arranque debe ser rápido: nada de inicializaciones costosas al arrancar.
- *Alternativa:* cliente en GitHub Pages y servidor aparte. Descartada en exploración porque exige CORS, orígenes WebSocket y dos despliegues.

## Risks / Trade-offs

- [El plan gratuito de Render cambia o desaparece] → Comprobar sus condiciones al implementar el despliegue. El `Dockerfile` es estándar y funciona igual en Koyeb o Google Cloud Run (con un máximo de 1 instancia); solo cambiaría `render.yaml`.
- [Arranque en frío de alrededor de un minuto tras un rato sin uso] → El cliente muestra "el servidor está despertando" (spec `cliente-web`). Para una demo, basta abrir la URL un poco antes.
- [Eliminaciones accidentales por una caída breve de red o por pasar el móvil a segundo plano] → Aceptado por decisión de producto. Los intervalos de ping son configurables, y un periodo de gracia queda para un cambio futuro.
- [Las partidas en memoria se pierden en un redeploy o reinicio] → Aceptado. Conviene no desplegar mientras alguien juega.
- [El proxy de Render cierra WebSockets inactivos] → El ping cada 25 s mantiene viva la conexión.
- [Un cliente malicioso podría mandar mensajes arbitrarios] → El servidor valida todo (apodos, anfitrión, estado de la sala, órdenes vía motor) y nunca confía en el cliente. Se limita el tamaño de los mensajes entrantes. No hay más defensas, como rate limiting, porque es una demo.
- [Que el JSON del protocolo se acople al modelo interno del motor] → Los DTO de `dto` se mapean explícitamente desde el modelo del motor, así que cambiar uno no rompe el otro sin que falle un test.

## Migration Plan

Es un módulo nuevo, así que no hay nada que migrar.
1. Desarrollar y probar en local con `./mvnw -pl servidor -am package -DskipTests && java -jar servidor/target/archipielago-servidor.jar` y abrir `http://localhost:8080` en dos navegadores.
2. Hacer push a `main` con `Dockerfile` y `render.yaml`.
3. En Render, crear el servicio desde "New > Blueprint" eligiendo el repo. Los siguientes pushes despliegan solos.
4. Rollback: "Rollback" en el panel de Render al deploy anterior, o revertir el commit.
