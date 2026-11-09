package uy.edu.um.pa.clasificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Pruebas unitarias de la resolucion de generos. */
class GeneroTest {

    @ParameterizedTest
    @ValueSource(strings = {"rpg", "RPG", " Rpg "})
    @DisplayName("Resuelve el genero sin importar mayusculas ni espacios")
    void resuelveIgnorandoFormato(String texto) {
        assertEquals(Genero.RPG, Genero.desde(texto).orElseThrow());
    }

    @Test
    @DisplayName("Resuelve el genero ignorando las tildes")
    void resuelveIgnorandoTildes() {
        assertEquals(Genero.ACCION, Genero.desde("Acción").orElseThrow());
        assertEquals(Genero.SIMULACION, Genero.desde("simulación").orElseThrow());
    }

    @Test
    @DisplayName("Devuelve vacio para un genero inexistente o nulo")
    void generoDesconocido() {
        assertTrue(Genero.desde("Carreras espaciales").isEmpty());
        assertTrue(Genero.desde(null).isEmpty());
        assertTrue(Genero.desde("   ").isEmpty());
    }

    @Test
    @DisplayName("Cada genero expone una etiqueta legible")
    void etiquetas() {
        assertEquals("Accion", Genero.ACCION.getEtiqueta());
        assertEquals("RPG", Genero.RPG.getEtiqueta());
    }
}
