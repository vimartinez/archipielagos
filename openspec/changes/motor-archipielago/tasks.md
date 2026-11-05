# Tasks

## 1. Estructura del proyecto

- [ ] 1.1 Crear el pom padre `archipielago` (groupId `com.mijuego`, Java 21, packaging pom) con el módulo `motor` y la gestión de dependencias de test (JUnit 5 BOM, AssertJ, Mockito, surefire); verificar que el módulo `motor` aparece en el reactor
- [ ] 1.2 Añadir Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) y verificar que `./mvnw -v` muestra Maven y Java 21
- [ ] 1.3 Crear el módulo `motor` con su pom (sin dependencias de runtime) y el paquete `com.mijuego.archipielago.motor`; verificar que `./mvnw -q verify` termina con éxito
- [ ] 1.4 Añadir `.gitignore` (target/, IDE) y un `README.md` con cómo compilar y testear con el wrapper; verificar que los comandos del README funcionan tal cual

## 2. Mapa

- [ ] 2.1 Implementar `IslaId`, `Ruta` y `Mapa` con las 13 islas, las 24 rutas bidireccionales con sus distancias y la asignación de salidas para 2, 3 y 4 jugadores (spec `mapa-archipielago`); verificar que compila con `./mvnw -q compile`
- [ ] 2.2 Escribir tests de `Mapa` que cubran todos los escenarios de `mapa-archipielago` (islas, rutas y distancias, ausencia de rutas, recuento de 24, simetría por rotación, salidas según jugadores); verificar que `./mvnw test` pasa en verde

## 3. Modelo y creación de partida

- [ ] 3.1 Implementar los records inmutables del modelo (`ConfiguracionPartida`, `Jugador`, `EstadoIsla`, `Flota`, `Orden`, `EstadoPartida`) y `ReglaInfringidaException`; verificar que compila
- [ ] 3.2 Implementar `Partida.crear` y `estado()`: validar jugadores (2-4, apodos no vacíos y únicos) y límite de rondas (10-50, 25 por defecto), y construir el estado inicial (ronda 1, 5 barcos en cada salida, el resto neutral vacío); verificar que compila
- [ ] 3.3 Escribir tests de los requisitos "Creación de partida", "Estado inicial" y "Estado público de la partida" de `ciclo-de-partida`; verificar que `./mvnw test` pasa en verde

## 4. Órdenes

- [ ] 4.1 Implementar `Partida.enviarOrdenes` con validación todo-o-nada (origen propio al inicio de la ronda, ruta directa, al menos 1 barco, suma por origen dentro de lo disponible, jugador activo, partida en curso) y sustitución de las órdenes previas; verificar que compila
- [ ] 4.2 Escribir tests de los requisitos "Envío de órdenes" y "Validación de órdenes" de `resolucion-de-ronda`, más el rechazo de órdenes de un jugador eliminado o en una partida terminada; verificar que `./mvnw test` pasa en verde

## 5. Movimiento de flotas

- [ ] 5.1 Implementar en `ResolutorRonda` las fases de salida, avance y llegada (las flotas se crean con la distancia de la ruta y se cruzan sin interactuar); verificar que compila
- [ ] 5.2 Escribir tests de los requisitos "Salida, avance y llegada de flotas" y "Flotas que se cruzan" (distancias 1 y 3, cruce en la misma ruta, vaciar una isla); verificar que `./mvnw test` pasa en verde

## 6. Combate, captura y producción

- [ ] 6.1 Implementar el combate como algoritmo único según design D5 (suma de fuerzas, descarte de fuerzas a 0, máximo único, empate con o sin defensor, refuerzo), la captura y la producción de +1 por isla de jugador activo; verificar que compila
- [ ] 6.2 Escribir tests de los requisitos "Combate en una isla", "Captura de islas" y "Producción" con todos sus escenarios; verificar que `./mvnw test` pasa en verde

## 7. Eliminación, fin de partida e informe

- [ ] 7.1 Implementar la fase final de la ronda: eliminación por quedarse sin fuerzas, fin por último jugador en pie (incluido el empate por eliminación simultánea), fin por límite de rondas (islas, después barcos totales, después victoria compartida), avance de ronda y limpieza de órdenes; verificar que compila
- [ ] 7.2 Implementar `Partida.eliminarPorDesconexion` (descartar órdenes pendientes, islas a neutrales vacías, quitar flotas, fin inmediato si queda un solo jugador) y el rechazo de órdenes y resoluciones en una partida terminada; verificar que compila
- [ ] 7.3 Implementar `InformeRonda` (combates con fuerzas, ganador y barcos restantes, capturas, eliminados, ganadores) devuelto por `Partida.resolverRonda`; verificar que compila
- [ ] 7.4 Escribir tests de los requisitos de eliminación, fin de partida y "Partida terminada" de `ciclo-de-partida`, y de "Orden de las fases de resolución" e "Informe de ronda" de `resolucion-de-ronda` (incluido el determinismo); verificar que `./mvnw test` pasa en verde

## 8. Integración

- [ ] 8.1 Escribir un test de partida completa de 3 jugadores, solo a través de la API de `Partida`, que recorra varias rondas con movimientos, combates, una eliminación y el fin por límite de rondas; verificar que `./mvnw verify` pasa en verde
- [ ] 8.2 Ejecutar `openspec validate motor-archipielago --strict` y verificar que el cambio sigue siendo válido
