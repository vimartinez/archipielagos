## 1. Módulo servidor

- [x] 1.1 Añadir el módulo `servidor` al pom padre, con el BOM de Spring Boot 4.x y las dependencias `motor` y `spring-boot-starter-websocket`. Verificar que `./mvnw -pl servidor -am package` genera un jar ejecutable.
- [x] 1.2 Crear la aplicación Spring Boot con `server.port=${PORT:8080}` y un `index.html` provisional en `static/`. Verificar que `./mvnw -pl servidor -am spring-boot:run` responde en `http://localhost:8080/`.

## 2. Núcleo de salas (Java puro)

- [x] 2.1 Definir los puertos `Notificador` y `Planificador` y sus dobles de test (un notificador que graba los mensajes y un planificador manual que avanza el tiempo). Verificar con un test trivial que los dobles funcionan.
- [x] 2.2 Implementar en `GestorSalas` la generación de códigos (alfabeto sin `0`, `O`, `1`, `I`, reintento si hay colisión) y la creación de salas. Verificar con tests: formato del código, unicidad con un generador forzado a colisionar y validación del apodo al crear.
- [x] 2.3 Implementar `unirse` en `Sala`: código insensible a mayúsculas, apodo recortado de 1 a 20 caracteres y único sin distinguir mayúsculas, sala llena, partida empezada, código inexistente. Verificar que pasan los tests de cada escenario de "Unirse a una sala".
- [x] 2.4 Implementar `configurarLimite` y `empezar`, solo para el anfitrión, con el límite entre 10 y 50 y de 2 a 4 jugadores. Al empezar se crea la `Partida` en orden de llegada y se programa el plazo de la ronda 1. Verificar con tests los escenarios de "Configurar el límite de rondas" y "Empezar la partida".
- [x] 2.5 Implementar la salida de la sala en espera: relevo del anfitrión al más antiguo y eliminación de la sala vacía. Verificar con tests los escenarios de "Salir de una sala en espera".
- [x] 2.6 Implementar `enviarOrdenes` y `confirmar` durante la partida. Las órdenes se validan con el motor, el error llega solo al emisor y no se aceptan cambios tras confirmar. Verificar con tests los escenarios de "Envío de órdenes en línea" y "Confirmación de órdenes", incluido que las órdenes no se envían a otros jugadores.
- [x] 2.7 Implementar la resolución de la ronda por plazo vencido y por confirmación de todos los activos, ignorando los vencimientos de rondas ya resueltas. Tras resolver se difunden el informe y el nuevo estado, y se programa el siguiente plazo. Verificar con el planificador manual los escenarios de "Reloj de la ronda", "Resolución anticipada" y "Difusión del estado y del informe".
- [x] 2.8 Implementar la desconexión durante la partida: eliminación vía motor, resolución si los restantes ya confirmaron y fin si queda uno. Implementar también los espectadores (los eliminados siguen recibiendo y no pueden ordenar). Verificar con tests los escenarios de "Eliminación por desconexión durante la partida" y "Espectadores".
- [x] 2.9 Implementar el fin de la partida: difusión de ganadores, rechazo de órdenes posteriores, reloj detenido, sala eliminada al irse el último y descarte si todos se van en curso. Verificar con tests los escenarios de "Fin de la partida en línea".

## 3. Protocolo y WebSocket

- [x] 3.1 Crear los DTO del protocolo (D4) y su conversión desde `EstadoPartida`, `InformeRonda` y el mapa del motor. Verificar con tests de serialización JSON que un estado con flotas en tránsito y un informe con combate producen los campos esperados.
- [x] 3.2 Implementar `JuegoWebSocketHandler` en `/ws`. Traduce los mensajes a comandos del núcleo, implementa `Notificador`, mapea sesiones a sala y apodo, invoca `desconectar` al cerrarse la sesión, limita el tamaño de los mensajes y responde `error` ante JSON inválido. Verificar con un test que un mensaje con `tipo` desconocido recibe un `error`.
- [x] 3.3 Añadir el ping periódico (25 s) y el cierre de las sesiones sin pong (60 s), con intervalos configurables. Verificar con un test que una sesión sin pong se cierra y su jugador queda eliminado.
- [x] 3.4 Escribir un test de integración con dos clientes WebSocket reales contra el servidor arrancado en un puerto aleatorio, con un plazo de ronda corto. El test crea la sala, se une, empieza, envía órdenes y confirma ambos, recibe el informe y desconecta a uno. Verificar que el otro recibe la victoria y que `./mvnw verify` pasa.

## 4. Cliente web

- [x] 4.1 Crear `index.html`, `estilos.css`, `app.js` (máquina de pantallas) y `conexion.js`. `conexion.js` gestiona la URL `ws` o `wss` según `location`, el aviso de "servidor despertando" a los 3 s y el aviso de conexión perdida con vuelta al inicio. Verificar en el navegador que con el servidor parado se ve el aviso y que, al arrancarlo, conecta.
- [x] 4.2 Implementar la pantalla de inicio y la sala de espera: crear y unirse, errores sin perder lo escrito, código copiable, lista con el anfitrión marcado, y controles de límite y de empezar solo para el anfitrión. Verificar con dos navegadores los escenarios de "Pantalla de inicio" y "Sala de espera".
- [x] 4.3 Implementar `mapa.js`: SVG con coordenadas fijas y simétricas de las 13 islas, rutas con su distancia, colores por jugador (gris para las neutrales) y flotas en tránsito sobre su ruta con barcos y rondas restantes. Verificar en una partida real que las flotas enviadas aparecen y avanzan cada ronda.
- [x] 4.4 Implementar `ordenes.js`: origen propio, destinos solo conectados, barcos acotados a los disponibles, lista con borrado, envío al servidor en cada cambio, errores junto a la lista y "Confirmar" con bloqueo hasta la siguiente ronda. Verificar en el navegador los escenarios de "Dar y confirmar órdenes".
- [x] 4.5 Mostrar la ronda y el límite, la cuenta atrás desde `msRestantes`, los jugadores confirmados y el informe de la última ronda en texto legible. Verificar en el navegador los escenarios de "Reloj y confirmaciones visibles" e "Informe de ronda visible".
- [x] 4.6 Implementar la vista de espectador ("Has sido eliminado", sin controles) y la pantalla final con ganadores, clasificación y vuelta al inicio. Verificar jugando una partida con límite de 10 rondas hasta el final y con un jugador eliminado.

## 5. Despliegue y documentación

- [x] 5.1 Crear el `Dockerfile` multi-stage (D8) y un `.dockerignore`. Verificar que `docker build` y `docker run -e PORT=8080 -p 8080:8080` sirven el juego en local, si Docker está disponible. Si no lo está, verificarlo en el primer deploy de Render.
- [ ] 5.2 Crear `render.yaml` (servicio web Docker, plan free, autodeploy desde `main`, health check en `/`) y comprobar las condiciones vigentes del plan gratuito de Render. Verificar que el blueprint se valida en Render.
- [x] 5.3 Actualizar el README: módulos, ejecución en local, cómo jugar y pasos para desplegar en Render, incluidas las limitaciones del plan gratuito. Verificar siguiendo las instrucciones desde un clon limpio.
- [ ] 5.4 Prueba final en producción: desplegar en Render, abrir la URL pública en dos navegadores (uno de ellos en otra red, si es posible) y jugar una partida completa con resolución anticipada y por plazo. Verificar que funciona de principio a fin y anotar la URL en el README.
