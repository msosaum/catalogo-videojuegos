package uy.edu.um.pa.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Pruebas unitarias de la persistencia en archivo JSON. */
class RepositorioJuegosArchivoTest {

    @Test
    @DisplayName("Crea el archivo y conserva los juegos entre dos instancias del repositorio")
    void persisteEntreInstancias(@TempDir Path carpeta) throws Exception {
        Path archivo = carpeta.resolve("juegos.json");

        RepositorioJuegosArchivo primero = new RepositorioJuegosArchivo(archivo.toString());
        primero.guardar(new Juego("1", "Minecraft", "Simulacion", 2011, null));
        primero.guardar(new Juego("2", "Hades", "RPG", 2020, null));

        assertTrue(Files.exists(archivo));

        RepositorioJuegosArchivo segundo = new RepositorioJuegosArchivo(archivo.toString());

        assertEquals(2, segundo.listarTodos().size());
        assertEquals("Minecraft", segundo.buscarPorId("1").orElseThrow().getTitulo());
    }

    @Test
    @DisplayName("Eliminar y vaciar quedan reflejados en el archivo")
    void eliminaYVacia(@TempDir Path carpeta) {
        Path archivo = carpeta.resolve("juegos.json");
        RepositorioJuegosArchivo repositorio = new RepositorioJuegosArchivo(archivo.toString());
        repositorio.guardar(new Juego("1", "Mario Kart 8", "Deportes", 2014, null));
        repositorio.guardar(new Juego("2", "Gran Turismo", "Deportes", 1997, null));

        assertTrue(repositorio.eliminar("1"));
        assertEquals(1, new RepositorioJuegosArchivo(archivo.toString()).listarTodos().size());

        repositorio.vaciar();
        assertEquals(0, new RepositorioJuegosArchivo(archivo.toString()).listarTodos().size());
    }

    @Test
    @DisplayName("Si el archivo no existe arranca con el catalogo vacio")
    void archivoInexistente(@TempDir Path carpeta) {
        RepositorioJuegosArchivo repositorio =
                new RepositorioJuegosArchivo(carpeta.resolve("no-existe.json").toString());

        assertEquals(0, repositorio.listarTodos().size());
    }
}
