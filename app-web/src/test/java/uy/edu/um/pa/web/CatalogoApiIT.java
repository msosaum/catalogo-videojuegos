package uy.edu.um.pa.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uy.edu.um.pa.catalogo.RepositorioJuegos;

/**
 * Pruebas de INTEGRACION: levantan la aplicacion completa (los dos modulos + la capa web
 * + la persistencia en archivo) sobre un puerto real y la ejercitan por HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "catalogo.archivo=target/it-data/catalogo-api.json")
class CatalogoApiIT {

    @Autowired
    private TestRestTemplate cliente;

    @Autowired
    private RepositorioJuegos repositorio;

    @BeforeEach
    void limpiar() {
        repositorio.vaciar();
    }

    @Test
    @DisplayName("Alta por API y recuperacion del juego creado")
    void altaYListado() {
        ResponseEntity<Map> alta = cliente.postForEntity("/api/juegos",
                Map.of("titulo", "Celeste", "genero", "Puzzle", "anio", 2018), Map.class);

        assertEquals(HttpStatus.CREATED, alta.getStatusCode());
        assertNotNull(alta.getBody().get("id"));
        assertEquals("Puzzle", alta.getBody().get("genero"));

        ResponseEntity<List> listado = cliente.getForEntity("/api/juegos", List.class);
        assertEquals(HttpStatus.OK, listado.getStatusCode());
        assertEquals(1, listado.getBody().size());
    }

    @Test
    @DisplayName("El filtro por genero recorre los dos modulos: catalogo y clasificacion")
    void filtradoPorGenero() {
        crear("Hades", "rpg", 2020);
        crear("Final Fantasy VII", "RPG", 1997);
        crear("DOOM", "accion", 1993);

        ResponseEntity<List<Map<String, Object>>> rpgs = cliente.exchange("/api/juegos?genero=RPG",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() { });

        assertEquals(HttpStatus.OK, rpgs.getStatusCode());
        assertEquals(2, rpgs.getBody().size());
        assertTrue(rpgs.getBody().stream().allMatch(j -> "RPG".equals(j.get("genero"))));
    }

    @Test
    @DisplayName("Un genero inexistente devuelve 400 con el mensaje del modulo de clasificacion")
    void generoInvalido() {
        ResponseEntity<Map> respuesta = cliente.postForEntity("/api/juegos",
                Map.of("titulo", "Un juego", "genero", "roguelike", "anio", 2021), Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertTrue(respuesta.getBody().get("error").toString().contains("roguelike"));
    }

    @Test
    @DisplayName("Un titulo duplicado devuelve 400 con el mensaje del modulo de catalogo")
    void tituloDuplicado() {
        crear("DOOM", "Accion", 1993);

        ResponseEntity<Map> respuesta = cliente.postForEntity("/api/juegos",
                Map.of("titulo", "doom", "genero", "Accion", "anio", 2016), Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertTrue(respuesta.getBody().get("error").toString().contains("Ya existe"));
    }

    @Test
    @DisplayName("Las estadisticas cuentan los juegos por genero")
    void estadisticas() {
        crear("Hades", "RPG", 2020);
        crear("Final Fantasy VII", "RPG", 1997);
        crear("Age of Empires II", "Estrategia", 1999);

        ResponseEntity<Map<String, Integer>> respuesta = cliente.exchange("/api/estadisticas",
                HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Integer>>() { });

        assertEquals(2, respuesta.getBody().get("RPG"));
        assertEquals(1, respuesta.getBody().get("Estrategia"));
    }

    @Test
    @DisplayName("Se puede eliminar un juego por API")
    void eliminacion() {
        ResponseEntity<Map> alta = crear("Portal", "Puzzle", 2007);
        String id = alta.getBody().get("id").toString();

        ResponseEntity<Void> borrado = cliente.exchange("/api/juegos/" + id, HttpMethod.DELETE, null, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, borrado.getStatusCode());
        assertEquals(0, cliente.getForEntity("/api/juegos", List.class).getBody().size());
    }

    private ResponseEntity<Map> crear(String titulo, String genero, int anio) {
        return cliente.postForEntity("/api/juegos",
                Map.of("titulo", titulo, "genero", genero, "anio", anio), Map.class);
    }
}
