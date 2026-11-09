# Catálogo de Videojuegos — Entregable 4 (CI/CD)

Programación Avanzada · Universidad de Montevideo · 2026
Integrantes: **Sosa · Lapetina**

Aplicación web Java/Spring Boot formada por dos módulos, con un pipeline declarativo
de Jenkins versionado junto al código (*pipeline as code*) que compila, prueba,
empaqueta y despliega automáticamente en un ambiente de producción simulado sobre
Docker cada vez que se detecta un cambio en el repositorio de GitHub.

---

## 1. Estructura del proyecto

```
Entregable4SosaLapetina/
├── pom.xml                     proyecto Maven multi-módulo (padre)
├── modulo-catalogo/            MÓDULO 1: alta, baja, consulta y persistencia de juegos
│   └── src/main/java/uy/edu/um/pa/catalogo/
│       ├── Juego.java                      entidad del dominio
│       ├── RepositorioJuegos.java          contrato de persistencia
│       ├── RepositorioJuegosArchivo.java   persistencia en archivo JSON
│       └── ServicioCatalogo.java           reglas de negocio del catálogo
├── modulo-clasificacion/       MÓDULO 2: géneros, filtrado y estadísticas
│   └── src/main/java/uy/edu/um/pa/clasificacion/
│       ├── Genero.java                     enum de géneros admitidos
│       └── ServicioClasificacion.java      normalización, filtros y conteos
├── app-web/                    aplicación Spring Boot que integra ambos módulos
│   └── src/main/java/uy/edu/um/pa/web/
│       ├── AplicacionCatalogo.java         punto de entrada
│       ├── ServicioAplicacion.java         capa de integración entre los 2 módulos
│       ├── CatalogoController.java         interfaz web (Thymeleaf)
│       └── ApiCatalogoController.java      API REST
├── Jenkinsfile                 pipeline declarativo (el corazón del entregable)
├── Dockerfile                  imagen de la aplicación
├── docker-compose.yml          ambiente de producción simulado
├── jenkins/                    imagen y configuración del servidor Jenkins
│   ├── Dockerfile              Jenkins + cliente Docker + plugins
│   ├── plugins.txt             plugins instalados
│   ├── casc.yaml               Configuration as Code (usuario admin + job)
│   └── docker-compose.yml      servidor Jenkins
├── scripts/                    scripts de apoyo
└── docs/                       informe técnico en PDF
```

## 2. Requisitos

- **Docker Desktop** (único requisito obligatorio: Jenkins y la aplicación corren en contenedores).
- **JDK 21 o superior** si se quiere compilar fuera de Docker. Maven **no** hace falta:
  el proyecto incluye el *Maven Wrapper* (`./mvnw`).

## 3. Ejecución rápida

### 3.1 Correr la aplicación en desarrollo

```bash
./scripts/correr-dev.sh
```
→ http://localhost:8090 (ambiente `DESARROLLO`, datos en `./datos/juegos.json`)

El script hace dos pasos, y los dos hacen falta: primero `./mvnw -DskipTests install`,
que deja los jars de los módulos en el repositorio local, y después
`./mvnw -pl app-web spring-boot:run`. No sirve un único `./mvnw -pl app-web -am
spring-boot:run`, porque con `-am` Maven intenta ejecutar el goal también sobre el POM
padre, que no tiene clase main.

### 3.2 Correr todas las pruebas

```bash
./scripts/probar-todo.sh        # equivale a ./mvnw clean verify
```

### 3.3 Levantar Jenkins

```bash
export REPO_URL=https://github.com/USUARIO/catalogo-videojuegos.git
./scripts/levantar-jenkins.sh
```
→ http://localhost:8080 — usuario `admin`, contraseña `admin`.
El job `catalogo-videojuegos` ya queda creado apuntando a `REPO_URL`.

### 3.4 Desplegar a producción sin Jenkins (opcional, para probar la imagen)

```bash
./scripts/desplegar-local.sh
```
→ http://localhost:8091 (ambiente `PRODUCCIÓN`)

### 3.5 Ver el estado / detener todo

```bash
./scripts/estado.sh
./scripts/detener-todo.sh
```

## 4. Ambientes

| Ambiente | Cómo se levanta | URL | Datos |
|---|---|---|---|
| Desarrollo | `./scripts/correr-dev.sh` | http://localhost:8090 | `./datos/juegos.json` |
| Pruebas | lo levanta Maven durante las pruebas de integración | puerto aleatorio | `target/it-data/` |
| Producción simulada | la despliega el pipeline de Jenkins | http://localhost:8091 | volumen Docker `catalogo-datos-prod` |

La aplicación muestra en la cabecera el ambiente en el que está corriendo y la versión,
para que en la defensa se vea con claridad cuál es cuál.

## 5. El pipeline

El `Jenkinsfile` define estas etapas:

| Etapa | Qué hace |
|---|---|
| Checkout | Clona el repositorio y registra commit, autor y mensaje |
| Compilar | `./mvnw -DskipTests clean install` (install, no package: ver 6.2) |
| Pruebas unitarias | `./mvnw surefire:test` (`*Test.java`) + publicación de resultados JUnit |
| Pruebas de integración | `./mvnw failsafe:integration-test failsafe:verify` (`*IT.java`) + resultados JUnit |
| Archivar artefacto | Guarda `app-web.jar` como artefacto del build |
| Construir imagen Docker | `docker build -t catalogo-videojuegos:${BUILD_NUMBER}` |
| Desplegar a producción | Reemplaza el contenedor `catalogo-prod` por la nueva imagen |
| Smoke test | Verifica `/actuator/health` y `/api/juegos` del contenedor recién desplegado |

**Disparadores:** webhook de GitHub (`githubPush`) y, como red de seguridad cuando
Jenkins corre en localhost y GitHub no puede alcanzarlo, *polling* cada 2 minutos
(`pollSCM('H/2 * * * *')`).

**Ante un fallo:** si falla cualquier etapa anterior al despliegue, producción queda
intacta sirviendo la versión anterior. Si el despliegue se hizo pero el smoke test
falla, el bloque `post { failure }` hace *rollback* a la imagen anterior.

## 6. Configurar el webhook de GitHub (opcional)

Jenkins corre en `localhost`, así que GitHub no puede alcanzarlo desde internet.
Dos opciones:

1. **Polling (por defecto, no requiere configuración):** Jenkins consulta el
   repositorio cada 2 minutos y arranca el build si hubo cambios.
2. **Webhook real con un túnel:** exponer Jenkins con `ngrok http 8080` y cargar
   `https://<tunel>/github-webhook/` en *Settings → Webhooks* del repositorio
   (content type `application/json`, evento `push`).

## 7. API REST

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/juegos` | Lista el catálogo (`?genero=RPG` filtra) |
| POST | `/api/juegos` | Alta: `{"titulo":"Hades","genero":"rpg","anio":2020}` |
| DELETE | `/api/juegos/{id}` | Elimina un juego |
| GET | `/api/generos` | Géneros admitidos |
| GET | `/api/estadisticas` | Cantidad de juegos por género |
| GET | `/actuator/health` | Salud de la aplicación (lo usa el smoke test) |

## 8. Apuntar Jenkins al repositorio real

La URL del repositorio que Jenkins clona se define con la variable `REPO_URL`:

```bash
cd jenkins
docker compose down
REPO_URL=https://github.com/USUARIO/catalogo-videojuegos.git docker compose up -d
```

Al arrancar, *Configuration as Code* vuelve a aplicar `jenkins/casc.yaml` y actualiza el
job con la URL nueva. El historial de builds y la caché de Maven se conservan porque
viven en el volumen `jenkins-home`.

Si el repositorio es privado, hay que agregar en Jenkins una credencial de tipo
*Username with password* con un *personal access token* de GitHub y seleccionarla en la
configuración del job.

## 9. Documentación

- `docs/Informe_Entregable4_SosaLapetina.pdf` — informe técnico (arquitectura,
  decisiones, instructivo, errores del pipeline y conclusiones).
- `docs/GUIA_DEFENSA.md` — guion paso a paso para la presentación en clase.
- `docs/informe.md` y `docs/generar-pdf.sh` — fuente del informe y script que lo
  regenera en PDF.
