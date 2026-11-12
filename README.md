# Archipiélago

Juego web de estrategia multijugador por turnos simultáneos. Proyecto demo de Spec-Driven Development con Claude Code y OpenSpec.

**Jugar:** <https://archipielagos-production.up.railway.app/>

## Módulos

- `motor`: motor de reglas en Java puro, sin dependencias de runtime. Su fachada pública es `com.mijuego.archipielago.motor.Partida`.
- `servidor`: servidor Spring Boot que gestiona las salas, las rondas con reloj y las desconexiones, y habla con el navegador por WebSocket en `/ws`. También sirve el cliente web: HTML, CSS y JS sin framework, en `servidor/src/main/resources/static/`.

## Requisitos

- JDK 21 o superior. El código se compila con `--release 21`.
- No hace falta instalar Maven: el proyecto incluye Maven Wrapper. La primera ejecución descarga Maven, así que necesita conexión.

## Compilar y testear

```sh
./mvnw -v        # comprueba Maven y Java
./mvnw compile   # compila
./mvnw test      # tests unitarios
./mvnw verify    # además, tests de integración (*IT) y empaquetado
```

En Windows usa `mvnw.cmd` en lugar de `./mvnw`.

Para ejecutar solo los tests de un módulo: `./mvnw -pl motor test`, o `./mvnw -pl servidor -am test`.

## Ejecutar en local

```sh
./mvnw -pl servidor -am package -DskipTests
java -jar servidor/target/archipielago-servidor.jar
```

Abre <http://localhost:8080> en dos o más navegadores, o en ventanas privadas para tener jugadores distintos:

1. Uno escribe su apodo y pulsa **Crear sala**. Recibe un código de 4 letras.
2. Los demás escriben su apodo y el código y pulsan **Unirse**. Pueden jugar de 2 a 4 personas.
3. El anfitrión elige el límite de rondas y pulsa **Empezar**.
4. En cada ronda hay 60 s para dar órdenes. Elige una isla tuya, un destino conectado y cuántos barcos enviar, o haz clic en tu isla y después en el destino. Pulsa **Confirmar** al terminar. La ronda se resuelve cuando vence el reloj o en cuanto todos confirman.

Si cierras la pestaña durante la partida quedas eliminado: no hay reconexión.

Parámetros configurables (propiedades o variables de entorno):

| Propiedad | Por defecto | Qué es |
|---|---|---|
| `server.port` / `PORT` | `8080` | Puerto HTTP |
| `archipielago.duracion-ronda` | `60s` | Tiempo de cada ronda |
| `archipielago.intervalo-ping` | `25s` | Cada cuánto se envía un ping a cada conexión |
| `archipielago.limite-pong` | `60s` | Tiempo sin respuesta tras el que una conexión se da por perdida |

Por ejemplo, para probar con rondas cortas: `java -jar servidor/target/archipielago-servidor.jar --archipielago.duracion-ronda=20s`.

### Con Docker

```sh
docker build -t archipielago .
docker run --rm -p 8080:8080 archipielago
```

## Despliegue en Railway

La versión pública está desplegada en [Railway](https://railway.com): <https://archipielagos-production.up.railway.app/>

Railway construye la imagen con el `Dockerfile` de la raíz e inyecta el puerto en la variable `PORT`, que el servidor ya usa. No hace falta ningún archivo de configuración adicional.

Para desplegar una copia propia:

1. Crea una cuenta en Railway y conecta tu cuenta de GitHub.
2. Crea un proyecto con **New Project > Deploy from GitHub repo** y elige este repositorio.
3. Cuando termine el build, en **Settings > Networking** pulsa **Generate Domain** para obtener una URL pública `https://<nombre>.up.railway.app`.

Cada push a la rama conectada (`main`) vuelve a desplegar. Para volver atrás, despliega un deploy anterior desde el panel del servicio o revierte el commit.

Limitaciones a tener en cuenta:

- **Las partidas viven en memoria.** Un nuevo despliegue o un reinicio del servicio borra las partidas en curso. Conviene no hacer push mientras alguien juega.
- **Una sola instancia.** Todos los jugadores de una sala tienen que llegar al mismo proceso, así que el servicio no debe escalarse a más de una réplica.
