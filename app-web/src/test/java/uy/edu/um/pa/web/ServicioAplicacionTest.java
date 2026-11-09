package uy.edu.um.pa.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uy.edu.um.pa.catalogo.Juego;
import uy.edu.um.pa.catalogo.RepositorioJuegos;
import uy.edu.um.pa.catalogo.ServicioCatalogo;
import uy.edu.um.pa.clasificacion.GeneroDesconocidoException;
import uy.edu.um.pa.clasificacion.ServicioClasificacion;

/**
 * Prueba unitaria de la capa de integracion, armada a mano (sin contexto de Spring)
 * para verificar que el genero pasa por el modulo de clasificacion antes del alta.
 */
class ServicioAplicacionTest {

    private ServicioAplicacion servicio;

    @BeforeEach
    void inicializar() {
        servicio = new ServicioAplicacion(new ServicioCatalogo(new RepositorioMemoria()), new ServicioClasificacion());
    }

    @Test
    @DisplayName("El genero se guarda en su forma canonica")
    void normalizaElGeneroAlRegistrar() {
        Juego juego = servicio.registrar(formulario("Hades", "rpg", 2020));

        assertEquals("RPG", juego.getGenero());
    }

    @Test
    @DisplayName("Un genero inexistente impide el alta")
    void rechazaGeneroDesconocido() {
        assertThrows(GeneroDesconocidoException.class,
                () -> servicio.registrar(formulario("Un juego", "roguelike", 2020)));
        assertEquals(0, servicio.cantidad());
    }

    @Test
    @DisplayName("La busqueda combina catalogo y clasificacion")
    void buscaPorGenero() {
        servicio.registrar(formulario("Hades", "rpg", 2020));
        servicio.registrar(formulario("DOOM", "accion", 1993));

        assertEquals(1, servicio.buscar("RPG").size());
        assertEquals(2, servicio.buscar(null).size());
        Map<String, Long> estadisticas = servicio.estadisticas();
        assertEquals(1L, estadisticas.get("Accion"));
    }

    private JuegoForm formulario(String titulo, String genero, int anio) {
        JuegoForm form = new JuegoForm();
        form.setTitulo(titulo);
        form.setGenero(genero);
        form.setAnio(anio);
        return form;
    }

    /** Repositorio en memoria para no tocar el disco durante la prueba unitaria. */
    private static class RepositorioMemoria implements RepositorioJuegos {
        private final Map<String, Juego> datos = new LinkedHashMap<>();

        @Override
        public List<Juego> listarTodos() {
            return new ArrayList<>(datos.values());
        }

        @Override
        public Optional<Juego> buscarPorId(String id) {
            return Optional.ofNullable(datos.get(id));
        }

        @Override
        public Juego guardar(Juego juego) {
            datos.put(juego.getId(), juego);
            return juego;
        }

        @Override
        public boolean eliminar(String id) {
            return datos.remove(id) != null;
        }

        @Override
        public void vaciar() {
            datos.clear();
        }
    }
}
