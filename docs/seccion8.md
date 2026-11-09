
# 8. Conclusiones

El trabajo dejó una separación muy clara entre dos cosas que en los entregables
anteriores venían mezcladas: **escribir código que funciona** y **tener un proceso que
garantice que lo que llega al ambiente de producción funciona**. La aplicación en sí es
deliberadamente modesta; el valor está en que ningún cambio llega a producción sin haber
pasado por compilación, pruebas unitarias de cada módulo, pruebas de integración de todo
el sistema y una verificación de salud posterior al despliegue, todo sin intervención
manual.

Algunas conclusiones concretas:

- **El pipeline como código vale la pena.** Tener el `Jenkinsfile` versionado junto al
  proyecto significa que el proceso de construcción evoluciona con el código y que
  cualquier cambio al pipeline queda registrado en el historial de Git, revisable como
  cualquier otro cambio. Llevar la misma idea a la configuración del servidor
  (`jenkins/casc.yaml`) hizo que levantar el entorno completo en otra máquina fuera un
  solo comando.
- **Separar pruebas unitarias de pruebas de integración cambia el diagnóstico.** Cuando
  falla una etapa, el tablero dice inmediatamente si se rompió la lógica de un módulo o
  la interacción entre módulos. Esa distinción no cuesta nada (es una convención de
  nombres) y ahorra tiempo cada vez que algo falla.
- **El ambiente de producción simulado en Docker obligó a pensar en el estado.** Como
  cada despliegue destruye y recrea el contenedor, los datos tenían que vivir fuera de
  él; separarlos en un volumen fue lo que convirtió el despliegue en algo repetible sin
  perder el catálogo cargado.

## 8.1 Dificultades encontradas

- **Docker dentro de Docker.** Entender que el contenedor de Jenkins no debía *contener*
  un daemon de Docker sino *usar* el del host fue el concepto que más costó. Una vez
  claro, se resolvió montando `/var/run/docker.sock`, pero obliga a correr el contenedor
  de Jenkins como `root` en Docker Desktop para poder escribir en ese socket.
- **La red entre contenedores.** Verificar el despliegue desde Jenkins no es trivial:
  dentro del contenedor de Jenkins, `localhost` no es el host, así que el smoke test no
  puede apuntar a `localhost:8091`. Se resolvió creando una red de Docker compartida
  (`catalogo-red`) a la que se conectan Jenkins y la aplicación, de modo que el smoke
  test llama al contenedor por su nombre (`http://catalogo-prod:8090`). Esto además hace
  que el pipeline funcione igual en Linux y en Docker Desktop.
- **El webhook de GitHub contra un Jenkins local.** GitHub no puede alcanzar
  `localhost:8080`. Se dejó el webhook configurado para cuando se expone Jenkins con un
  túnel, y el *polling* del SCM como mecanismo de respaldo para la demostración.
- **Tiempos del primer build.** La primera ejecución del pipeline es notablemente más
  lenta que las siguientes porque Maven baja todas las dependencias dentro del
  contenedor. Se mitigó dejando el repositorio local de Maven en el volumen persistente
  de Jenkins (`/var/jenkins_home/.m2/repository`), de modo que los builds siguientes
  reutilizan la caché.

## 8.2 Trabajo futuro

Lo primero que agregaríamos es un **ambiente intermedio de UAT** con una aprobación
manual en Jenkins (`input`) antes de promover a producción, que es el camino natural de
este pipeline. Después, publicar las imágenes en un registro en lugar de dejarlas en el
daemon local, y reemplazar el `docker run` del despliegue por un `docker compose up` con
el archivo versionado, para que la definición del ambiente de producción también sea
código.
