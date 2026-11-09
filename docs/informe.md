---
title: "Entregable 4 — Integración y Despliegue Continuo"
subtitle: "Catálogo de Videojuegos · Pipeline CI/CD con Jenkins"
author: "Sosa · Lapetina — Programación Avanzada, Universidad de Montevideo"
date: "Noviembre de 2026"
---

# 1. Introducción

Este entregable implementa el ciclo de vida completo de una aplicación web sencilla,
poniendo el foco —tal como pide la letra— **no en la funcionalidad ni en la apariencia
de la aplicación, sino en el proceso de CI/CD**.

La aplicación elegida es un **catálogo de videojuegos** compuesto por dos módulos
independientes. Sobre ella se construyó un pipeline declarativo de Jenkins, versionado
junto al código (*pipeline as code*), que ante cada cambio en el repositorio de GitHub
compila el sistema, corre las pruebas unitarias de cada módulo, corre las pruebas de
integración, empaqueta la aplicación, construye una imagen de Docker y la despliega en
un ambiente de producción simulado, verificando al final que la versión desplegada
responda correctamente.

**Repositorio:** el código, el `Jenkinsfile`, los `Dockerfile` y este informe viven en el
mismo repositorio de GitHub.

# 2. Arquitectura de la solución

## 2.1 Visión general

La solución tiene tres piezas que corren en contenedores separados sobre el mismo host:

```
                        push
   Desarrollador  ─────────────────▶  GitHub (repositorio)
        │                                    │
        │ docker compose                     │ webhook / polling
        ▼                                    ▼
   ┌──────────────────┐            ┌────────────────────────────┐
   │  Ambiente DEV    │            │   Jenkins (contenedor)     │
   │  localhost:8090  │            │   localhost:8080           │
   │  mvn spring-boot │            │   ejecuta el Jenkinsfile   │
   └──────────────────┘            └──────────────┬─────────────┘
                                                  │ docker build / docker run
                                                  │ (socket del host)
                                                  ▼
                                   ┌────────────────────────────┐
                                   │  Ambiente PRODUCCIÓN       │
                                   │  contenedor catalogo-prod  │
                                   │  localhost:8091            │
                                   │  volumen catalogo-datos    │
                                   └────────────────────────────┘
```

Jenkins y la aplicación comparten la red de Docker `catalogo-red`, lo que le permite al
pipeline verificar la salud del contenedor recién desplegado llamándolo por su nombre
(`http://catalogo-prod:8090/actuator/health`) sin depender de la configuración de red
del host.

## 2.2 Arquitectura de la aplicación

El sistema es un proyecto **Maven multi-módulo**. Cada módulo es un artefacto con su
propio `pom.xml`, su propio código y su propia suite de pruebas unitarias:

```
catalogo-videojuegos (pom padre)
│
├── modulo-catalogo ............ MÓDULO 1
│     Juego                      entidad del dominio
│     RepositorioJuegos          contrato de persistencia (interfaz)
│     RepositorioJuegosArchivo   implementación sobre archivo JSON
│     ServicioCatalogo           reglas de negocio: alta, baja, consulta
│
├── modulo-clasificacion ....... MÓDULO 2   (depende del módulo 1)
│     Genero                     enum de géneros admitidos
│     ServicioClasificacion      normalización, filtrado, estadísticas
│
└── app-web .................... APLICACIÓN (depende de los módulos 1 y 2)
      AplicacionCatalogo         arranque de Spring Boot
      ServicioAplicacion         integración entre ambos módulos
      CatalogoController         interfaz web (Thymeleaf)
      ApiCatalogoController      API REST
      ManejadorErroresApi        traducción de excepciones a HTTP 400
```

Las dependencias van en una sola dirección (`app-web → modulo-clasificacion → modulo-catalogo`),
lo que permite probar cada módulo por separado y hace visible, en el reactor de Maven,
el orden de construcción.

**Punto de integración entre los módulos.** El alta de un juego atraviesa los dos: el
género que escribe el usuario se valida y normaliza con `ServicioClasificacion`
(`"rpg"` → `"RPG"`, `"ACCIÓN"` → `"Accion"`) y recién entonces `ServicioCatalogo`
aplica sus propias reglas (título obligatorio y no duplicado, año entre 1958 y el año
actual + 5) y persiste. Las pruebas de integración ejercitan justamente ese camino.

## 2.3 Persistencia

La letra permite resolver la persistencia con una base de datos o con un archivo de
texto. Se eligió un **archivo JSON** (`datos/juegos.json`) escrito con Jackson. En el
contenedor de producción ese directorio es un **volumen de Docker**
(`catalogo-datos-prod`), de modo que el catálogo sobrevive a cada despliegue: el
pipeline destruye y recrea el contenedor, pero los datos quedan en el volumen.

## 2.4 Ambientes

| Ambiente | Cómo se levanta | URL | Datos |
|---|---|---|---|
| Desarrollo | `./mvnw -pl app-web -am spring-boot:run` | `localhost:8090` | `./datos/juegos.json` |
| Pruebas | Maven, durante las pruebas de integración | puerto aleatorio | `target/it-data/` |
| Producción simulada | desplegado por el pipeline de Jenkins | `localhost:8091` | volumen `catalogo-datos-prod` |

La aplicación muestra en la cabecera el ambiente activo y la versión, para que durante
la defensa quede claro qué instancia se está viendo.

# 3. El pipeline

## 3.1 Etapas

El `Jenkinsfile` es declarativo y está versionado en la raíz del repositorio:

| # | Etapa | Qué hace | Si falla |
|---|---|---|---|
| 1 | Checkout | Clona el repo y registra commit, autor y mensaje | No hay build |
| 2 | Compilar | `./mvnw -DskipTests clean package` | Producción intacta |
| 3 | Pruebas unitarias | `./mvnw surefire:test` (`*Test.java`), publica resultados JUnit | Producción intacta |
| 4 | Pruebas de integración | `./mvnw failsafe:integration-test failsafe:verify` (`*IT.java`) | Producción intacta |
| 5 | Archivar artefacto | Guarda `app-web.jar` con fingerprint | — |
| 6 | Imagen Docker | `docker build -t catalogo-videojuegos:${BUILD_NUMBER}` | Producción intacta |
| 7 | Desplegar | Reemplaza el contenedor `catalogo-prod` por la nueva imagen | Rollback |
| 8 | Smoke test | Verifica `/actuator/health` y `/api/juegos` | Rollback |

La separación entre las etapas 3 y 4 se logra con la convención de nombres de Maven:
**surefire** ejecuta los `*Test.java` (unitarios, sin contexto de Spring, en milisegundos)
y **failsafe** ejecuta los `*IT.java` (integración, levantan la aplicación completa en un
puerto real). Así el tablero de Jenkins muestra por separado qué tipo de prueba falló.

## 3.2 Disparadores

```groovy
triggers {
    githubPush()                // webhook de GitHub
    pollSCM('H/2 * * * *')      // red de seguridad cada 2 minutos
}
```

Jenkins corre en `localhost`, por lo que GitHub no puede alcanzarlo desde internet para
entregar el webhook. Se dejaron configurados los dos mecanismos: el webhook (que funciona
si se expone Jenkins con un túnel, por ejemplo `ngrok http 8080`) y el *polling*, que
consulta el repositorio cada dos minutos y arranca el build si detectó cambios. En la
defensa se usa el polling, que no depende de la red de la facultad.

## 3.3 Estrategia ante fallos

- **Falla una etapa anterior al despliegue** (compilación o pruebas): el pipeline se
  detiene, el contenedor de producción nunca se toca y sigue sirviendo la versión
  anterior. Jenkins marca el build en rojo e indica el autor del commit culpable.
- **Falla el smoke test posterior al despliegue**: el bloque `post { failure }` detecta
  que ya había una imagen anterior desplegada y hace **rollback**, volviendo a levantar
  el contenedor con la imagen previa.

Cada imagen se etiqueta con el número de build (`catalogo-videojuegos:17`), lo que hace
posible volver a cualquier versión anterior con un solo `docker run`.

## 3.4 Jenkins como código

El servidor Jenkins también está definido en el repositorio:

- `jenkins/Dockerfile`: parte de `jenkins/jenkins:lts-jdk21` y agrega el cliente de
  Docker, para que el pipeline pueda construir imágenes y levantar contenedores usando
  el daemon del host (patrón *Docker outside of Docker*, montando `/var/run/docker.sock`).
- `jenkins/plugins.txt`: los plugins que el pipeline necesita.
- `jenkins/casc.yaml`: *Configuration as Code*. Crea el usuario administrador y el job
  `catalogo-videojuegos` apuntando al repositorio, con sus disparadores. Esto evita tener
  que configurar Jenkins a mano y hace que el entorno sea reproducible en cualquier
  máquina del grupo.

Maven no se instala en la imagen: el proyecto incluye el **Maven Wrapper** (`./mvnw`),
de modo que el pipeline usa exactamente la misma versión de Maven (3.9.9) que se usa en
desarrollo.

# 4. Decisiones tomadas y justificación

## 4.1 Aplicación: catálogo de videojuegos

Se eligió el catálogo de videojuegos entre las opciones propuestas porque es el dominio
con el modelo más pequeño (un juego es título, género, año e imagen) y porque la
clasificación por género da naturalmente un **segundo módulo con entidad propia**, en
lugar de ser una función suelta. Como la letra aclara que la funcionalidad no se evalúa,
se priorizó un dominio chico que dejara el esfuerzo del lado del pipeline y que redujera
la cantidad de cosas que pueden fallar durante la demostración en vivo.

## 4.2 Lenguaje y framework: Java 21 + Spring Boot 3.3

Impuesto por la letra (Java con Spring). Dentro de eso se eligió **Spring Boot** por tres
razones concretas para un trabajo de CI/CD:

- produce un **jar ejecutable autocontenido**, que es exactamente lo que el pipeline
  necesita para armar una imagen de Docker mínima;
- trae **Spring Boot Actuator**, cuyo endpoint `/actuator/health` se usa como *smoke test*
  del despliegue y como `HEALTHCHECK` del contenedor;
- su soporte de pruebas (`@SpringBootTest` con puerto aleatorio) permite escribir pruebas
  de integración reales sin infraestructura adicional.

Se compila con `--release 21` (LTS) aunque el JDK instalado sea más nuevo, para que el
mismo bytecode corra en la imagen `eclipse-temurin:21-jre` que usa producción.

## 4.3 Maven multi-módulo

La letra pide "un sistema formado por varios módulos". Se usó un proyecto Maven
multi-módulo en lugar de un único proyecto con paquetes separados porque:

- obliga a que las dependencias entre módulos sean explícitas y unidireccionales;
- cada módulo tiene su propia suite de pruebas unitarias, y el reactor las ejecuta
  por separado: en el log del pipeline se ve qué módulo falló;
- es la estructura que después permite publicar los módulos como artefactos
  independientes.

Se agregó el **Maven Wrapper** al repositorio para que ni Jenkins ni los integrantes del
grupo tengan que instalar Maven, y para que todos usen la misma versión.

## 4.4 Pruebas: surefire para unitarias, failsafe para integración

En lugar de distinguir los dos tipos de prueba con etiquetas, se usó la convención
estándar de Maven: `*Test.java` lo ejecuta surefire y `*IT.java` lo ejecuta failsafe.
Es una decisión barata que da dos beneficios: el pipeline puede correr cada conjunto en
una etapa distinta sin recompilar, y Jenkins publica dos reportes JUnit separados.

Las pruebas unitarias no levantan Spring: usan un repositorio en memoria escrito a mano
como doble de prueba. Las de integración sí levantan la aplicación completa sobre un
puerto real y la consultan por HTTP, con un archivo de datos temporal
(`target/it-data/`) para no ensuciar los datos de desarrollo.

## 4.5 Persistencia en archivo JSON

Se descartó una base de datos porque habría agregado un contenedor más, credenciales y
migraciones al ambiente de producción simulado, sin aportar nada al objetivo del
entregable. El archivo JSON montado como volumen de Docker demuestra el punto importante
—que los datos sobreviven a los despliegues— con una fracción de la complejidad.

## 4.6 Ambiente de producción simulado en Docker

La letra admite una carpeta local, un contenedor u otra alternativa. Se eligió Docker
porque:

- el despliegue es **reproducible**: la misma imagen corre igual en cualquier máquina
  del grupo y en la de la defensa;
- permite versionar cada despliegue (`catalogo-videojuegos:${BUILD_NUMBER}`) y por lo
  tanto hacer *rollback* real;
- deja el ambiente de producción visiblemente separado del de desarrollo (otro puerto,
  otros datos, otra etiqueta en la cabecera de la página).

## 4.7 Jenkins en contenedor, configurado como código

Jenkins corre en un contenedor construido desde el repositorio. Para que el pipeline
pueda a su vez manejar contenedores se montó el socket del daemon del host
(*Docker outside of Docker*) en vez de usar *Docker in Docker*, que es más frágil y
pesado. La configuración del servidor (usuario administrador y definición del job) está
en `jenkins/casc.yaml` con el plugin *Configuration as Code*, de manera que levantar el
entorno completo en otra máquina sea un único comando y no una secuencia de clics.

# 5. Instructivo para ejecución

## 5.1 Requisitos

- **Docker Desktop** en ejecución (único requisito obligatorio).
- **JDK 21 o superior**, solo si se quiere compilar fuera de Docker. Maven no es
  necesario: el repositorio incluye `./mvnw`.

## 5.2 Clonar el repositorio

```bash
git clone https://github.com/USUARIO/catalogo-videojuegos.git
cd catalogo-videojuegos
```

## 5.3 Correr la aplicación en desarrollo

```bash
./mvnw -pl app-web -am spring-boot:run
```

La aplicación queda en <http://localhost:8090> con la etiqueta `DESARROLLO`.

## 5.4 Correr las pruebas

```bash
./scripts/probar-todo.sh      # equivale a ./mvnw clean verify
```

Ejecuta las pruebas unitarias de los tres módulos y después las de integración.

## 5.5 Levantar Jenkins

```bash
export REPO_URL=https://github.com/USUARIO/catalogo-videojuegos.git
./scripts/levantar-jenkins.sh
```

El script crea la red `catalogo-red` y levanta Jenkins en <http://localhost:8080>
(usuario `admin`, contraseña `admin`). El job `catalogo-videojuegos` ya queda creado
apuntando al repositorio indicado, con el *polling* activo.

## 5.6 Ejecutar el pipeline

Alcanza con hacer *push* al repositorio: Jenkins lo detecta en menos de dos minutos y
arranca el build. También se puede forzar con **Build Now** desde la interfaz.

Al terminar, la nueva versión queda desplegada en <http://localhost:8091> con la
etiqueta `PRODUCCIÓN`.

## 5.7 Comandos auxiliares

```bash
./scripts/estado.sh            # estado de los contenedores y salud de producción
./scripts/desplegar-local.sh   # desplegar a producción sin pasar por Jenkins
./scripts/detener-todo.sh      # detener Jenkins y producción
```
