package uy.edu.um.pa.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Year;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias del modulo de catalogo. */
class ServicioCatalogoTest {

    private ServicioCatalogo servicio;

    @BeforeEach
    void inicializar() {
        servicio = new ServicioCatalogo(new RepositorioEnMemoria());
    }

    @Test
    @DisplayName("Registra un juego valido y le asigna un identificador")
    void registraJuegoValido() {
        Juego juego = servicio.registrar("Celeste", "Plataformas", 2018, "https://ejemplo/celeste.png");

        assertNotNull(juego.getId());
        assertEquals("Celeste", juego.getTitulo());
        assertEquals(2018, juego.getAnio());
        assertEquals(1, servicio.cantidad());
    }

    @Test
    @DisplayName("Recorta los espacios del titulo y del genero")
    void normalizaEspacios() {
        Juego juego = servicio.registrar("  Tetris  ", "  Puzzle  ", 1984, null);

        assertEquals("Tetris", juego.getTitulo());
        assertEquals("Puzzle", juego.getGenero());
    }

    @Test
    @DisplayName("Rechaza un titulo vacio")
    void rechazaTituloVacio() {
        JuegoInvalidoException e = assertThrows(JuegoInvalidoException.class,
                () -> servicio.registrar("   ", "Accion", 2020, null));

        assertTrue(e.getMessage().contains("titulo"));
    }

    @Test
    @DisplayName("Rechaza un genero vacio")
    void rechazaGeneroVacio() {
        assertThrows(JuegoInvalidoException.class, () -> servicio.registrar("Pong", "", 1972, null));
    }

    @Test
    @DisplayName("Rechaza anios anteriores a 1958")
    void rechazaAnioDemasiadoAntiguo() {
        assertThrows(JuegoInvalidoException.class, () -> servicio.registrar("Juego", "Accion", 1900, null));
    }

    @Test
    @DisplayName("Rechaza anios demasiado lejanos en el futuro")
    void rechazaAnioFuturo() {
        int anioInvalido = Year.now().getValue() + 10;

        assertThrows(JuegoInvalidoException.class, () -> servicio.registrar("Juego", "Accion", anioInvalido, null));
    }

    @Test
    @DisplayName("No permite dos juegos con el mismo titulo, sin importar mayusculas")
    void rechazaTituloDuplicado() {
        servicio.registrar("DOOM", "Accion", 1993, null);

        assertThrows(JuegoInvalidoException.class, () -> servicio.registrar("doom", "Accion", 2016, null));
    }

    @Test
    @DisplayName("Elimina un juego existente y devuelve false si no existe")
    void eliminaJuego() {
        Juego juego = servicio.registrar("Portal", "Puzzle", 2007, null);

        assertTrue(servicio.eliminar(juego.getId()));
        assertEquals(0, servicio.cantidad());
        assertFalse(servicio.eliminar("id-inexistente"));
    }

    @Test
    @DisplayName("Busca un juego por su identificador")
    void buscaPorId() {
        Juego juego = servicio.registrar("Stardew Valley", "Simulacion", 2016, null);

        assertTrue(servicio.buscarPorId(juego.getId()).isPresent());
        assertTrue(servicio.buscarPorId("otro").isEmpty());
    }
}
