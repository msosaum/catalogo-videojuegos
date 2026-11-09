package uy.edu.um.pa.web;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uy.edu.um.pa.catalogo.Juego;
import uy.edu.um.pa.catalogo.ServicioCatalogo;
import uy.edu.um.pa.clasificacion.ServicioClasificacion;

/**
 * Capa de integracion entre los dos modulos: valida el genero con el modulo de
 * clasificacion antes de delegar el alta en el modulo de catalogo, y combina
 * ambos para las consultas filtradas.
 */
@Service
public class ServicioAplicacion {

    private final ServicioCatalogo catalogo;
    private final ServicioClasificacion clasificacion;

    public ServicioAplicacion(ServicioCatalogo catalogo, ServicioClasificacion clasificacion) {
        this.catalogo = catalogo;
        this.clasificacion = clasificacion;
    }

    /** Alta de un juego: el genero se normaliza con el modulo de clasificacion. */
    public Juego registrar(JuegoForm form) {
        String generoCanonico = clasificacion.normalizar(form.getGenero());
        return catalogo.registrar(form.getTitulo(), generoCanonico,
                form.getAnio() == null ? 0 : form.getAnio(), form.getImagenUrl());
    }

    public List<Juego> buscar(String genero) {
        return clasificacion.filtrarPorGenero(catalogo.listar(), genero);
    }

    public List<Juego> listarTodos() {
        return catalogo.listar();
    }

    public Map<String, Long> estadisticas() {
        return clasificacion.contarPorGenero(catalogo.listar());
    }

    public List<String> generosDisponibles() {
        return clasificacion.generosDisponibles();
    }

    public boolean eliminar(String id) {
        return catalogo.eliminar(id);
    }

    public int cantidad() {
        return catalogo.cantidad();
    }
}
