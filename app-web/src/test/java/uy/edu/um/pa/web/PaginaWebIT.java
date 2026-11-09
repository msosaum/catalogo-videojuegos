package uy.edu.um.pa.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uy.edu.um.pa.catalogo.RepositorioJuegos;
import uy.edu.um.pa.catalogo.ServicioCatalogo;

/** Pruebas de integracion de la interfaz web y del endpoint de salud que usa el pipeline. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "catalogo.archivo=target/it-data/pagina-web.json",
        "app.ambiente=PRUEBAS"
})
class PaginaWebIT {

    @Autowired
    private TestRestTemplate cliente;

    @Autowired
    private ServicioCatalogo catalogo;

    @Autowired
    private RepositorioJuegos repositorio;

    @BeforeEach
    void limpiar() {
        repositorio.vaciar();
    }

    @Test
    @DisplayName("La pagina principal renderiza el catalogo y el ambiente activo")
    void paginaPrincipal() {
        catalogo.registrar("Hollow Knight", "Aventura", 2017, null);

        ResponseEntity<String> respuesta = cliente.getForEntity("/", String.class);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertTrue(respuesta.getBody().contains("Hollow Knight"));
        assertTrue(respuesta.getBody().contains("PRUEBAS"));
    }

    @Test
    @DisplayName("La pagina filtrada por genero solo muestra los juegos de ese genero")
    void paginaFiltrada() {
        catalogo.registrar("Hades", "RPG", 2020, null);
        catalogo.registrar("DOOM", "Accion", 1993, null);

        String html = cliente.getForObject("/?genero=RPG", String.class);

        assertTrue(html.contains("Hades"));
        assertTrue(!html.contains(">DOOM<"));
    }

    @Test
    @DisplayName("El endpoint de salud responde UP (smoke test del despliegue)")
    void saludAplicacion() {
        ResponseEntity<String> salud = cliente.getForEntity("/actuator/health", String.class);

        assertEquals(HttpStatus.OK, salud.getStatusCode());
        assertTrue(salud.getBody().contains("UP"));
    }

    @Test
    @DisplayName("El alta desde el formulario web redirige al catalogo")
    void altaDesdeFormulario() {
        org.springframework.util.LinkedMultiValueMap<String, String> formulario =
                new org.springframework.util.LinkedMultiValueMap<>();
        formulario.add("titulo", "Celeste");
        formulario.add("genero", "Puzzle");
        formulario.add("anio", "2018");

        ResponseEntity<String> respuesta = cliente.postForEntity("/juegos", formulario, String.class);

        assertTrue(respuesta.getStatusCode().is3xxRedirection() || respuesta.getStatusCode().is2xxSuccessful());
        assertEquals(1, catalogo.cantidad());
    }
}
