package uy.edu.um.pa.clasificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uy.edu.um.pa.catalogo.Juego;

/** Pruebas unitarias del modulo de clasificacion. */
class ServicioClasificacionTest {

    private ServicioClasificacion servicio;
    private List<Juego> catalogo;

    @BeforeEach
    void inicializar() {
        servicio = new ServicioClasificacion();
        catalogo = List.of(
                new Juego("1", "DOOM", "Accion", 1993, null),
                new Juego("2", "Final Fantasy VII", "RPG", 1997, null),
                new Juego("3", "Hades", "RPG", 2020, null),
                new Juego("4", "Age of Empires II", "Estrategia", 1999, null));
    }

    @Test
    @DisplayName("Normaliza un genero escrito de cualquier forma")
    void normalizaGenero() {
        assertEquals("RPG", servicio.normalizar("rpg"));
        assertEquals("Accion", servicio.normalizar("ACCIÓN"));
    }

    @Test
    @DisplayName("Falla al normalizar un genero inexistente")
    void falloAlNormalizar() {
        GeneroDesconocidoException e =
                assertThrows(GeneroDesconocidoException.class, () -> servicio.normalizar("Metroidvania"));

        assertTrue(e.getMessage().contains("Metroidvania"));
    }

    @Test
    @DisplayName("Reconoce si un genero es valido")
    void validaGenero() {
        assertTrue(servicio.esGeneroValido("terror"));
        assertFalse(servicio.esGeneroValido("roguelike"));
    }

    @Test
    @DisplayName("Filtra el catalogo por genero")
    void filtraPorGenero() {
        List<Juego> rpgs = servicio.filtrarPorGenero(catalogo, "rpg");

        assertEquals(2, rpgs.size());
        assertTrue(rpgs.stream().allMatch(j -> j.getGenero().equalsIgnoreCase("RPG")));
    }

    @Test
    @DisplayName("Sin genero devuelve el catalogo completo")
    void filtroVacioDevuelveTodo() {
        assertEquals(4, servicio.filtrarPorGenero(catalogo, null).size());
        assertEquals(4, servicio.filtrarPorGenero(catalogo, "  ").size());
    }

    @Test
    @DisplayName("Un genero inexistente no devuelve resultados")
    void filtroConGeneroDesconocido() {
        assertTrue(servicio.filtrarPorGenero(catalogo, "roguelike").isEmpty());
    }

    @Test
    @DisplayName("Cuenta los juegos de cada genero ordenando de mayor a menor")
    void cuentaPorGenero() {
        Map<String, Long> conteo = servicio.contarPorGenero(catalogo);

        assertEquals(3, conteo.size());
        assertEquals(2L, conteo.get("RPG"));
        assertEquals(1L, conteo.get("Accion"));
        assertEquals("RPG", conteo.keySet().iterator().next());
    }

    @Test
    @DisplayName("Agrupa por genero ordenando cada grupo por anio descendente")
    void agrupaPorGenero() {
        Map<String, List<Juego>> grupos = servicio.agruparPorGenero(catalogo);

        assertEquals(3, grupos.size());
        assertEquals("Hades", grupos.get("RPG").get(0).getTitulo());
    }

    @Test
    @DisplayName("Expone la lista de generos disponibles para el formulario")
    void generosDisponibles() {
        List<String> generos = servicio.generosDisponibles();

        assertEquals(Genero.values().length, generos.size());
        assertTrue(generos.contains("Estrategia"));
    }
}
