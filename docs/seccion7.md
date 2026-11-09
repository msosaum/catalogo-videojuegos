
# 7. Demostración: los dos casos de la defensa

## 7.1 Caso 1 — Integración exitosa

1. Se modifica algo visible del código, por ejemplo agregar un género nuevo al enum
   `Genero` o cambiar el título de la página.
2. `git commit` y `git push` a `main`.
3. En menos de dos minutos Jenkins detecta el cambio (o se dispara el webhook) y arranca
   el build. El tablero muestra las ocho etapas pasando en verde.
4. Al terminar la etapa *Desplegar a producción*, el contenedor `catalogo-prod` se
   recrea con la imagen nueva y el *smoke test* confirma que responde.
5. Se recarga <http://localhost:8091> y se ve el cambio en producción, con el número de
   versión actualizado en la cabecera. Los juegos cargados siguen estando: el volumen de
   datos sobrevivió al despliegue.

## 7.2 Caso 2 — Fallo en las pruebas

1. Se introduce un error a propósito. Por ejemplo, en `ServicioClasificacion.normalizar`
   se devuelve el texto sin normalizar, lo que rompe la prueba unitaria
   `normalizaGenero` y la prueba de integración `filtradoPorGenero`.
2. Se hace *push*. Jenkins arranca el build.
3. Las etapas *Checkout* y *Compilar* pasan; la etapa **Pruebas unitarias** falla.
   El pipeline se detiene ahí: **no se construye la imagen ni se despliega nada**.
4. Jenkins muestra en rojo la etapa que falló, publica el reporte JUnit con el nombre
   exacto de la prueba (`ServicioClasificacionTest.normalizaGenero`, esperaba `"RPG"` y
   recibió `"rpg"`) e informa el autor y el mensaje del commit responsable.
5. **Producción no se vio afectada**: <http://localhost:8091> sigue sirviendo la versión
   anterior, que es el comportamiento que se busca.
6. Procedimiento de resolución: el desarrollador corrige el código (o revierte el commit
   con `git revert`), vuelve a hacer *push*, y el pipeline corre de nuevo. Recién cuando
   todas las pruebas pasan, la nueva versión llega a producción.

Este segundo caso es el que justifica tener el pipeline: el error quedó contenido entre
el *push* y el despliegue, sin intervención manual y sin que nadie tuviera que acordarse
de correr las pruebas.
