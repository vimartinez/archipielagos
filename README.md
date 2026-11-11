# Archipiélago

Juego web de estrategia multijugador por turnos simultáneos. Proyecto demo de Spec-Driven Development con Claude Code y OpenSpec.

## Módulos

- `motor`: motor de reglas en Java puro (sin dependencias de runtime). Su fachada pública es `com.mijuego.archipielago.motor.Partida`.

## Requisitos

- JDK 21 o superior (el código se compila con `--release 21`).
- No hace falta instalar Maven: el proyecto incluye Maven Wrapper. La primera ejecución descarga Maven, así que necesita conexión.

## Compilar y testear

```sh
./mvnw -v        # comprueba Maven y Java
./mvnw compile   # compila
./mvnw test      # ejecuta los tests
./mvnw verify    # compila, testea y empaqueta
```

En Windows usa `mvnw.cmd` en lugar de `./mvnw`.

Para ejecutar solo los tests del motor: `./mvnw -pl motor test`.
