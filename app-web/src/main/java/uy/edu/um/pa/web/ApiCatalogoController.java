package uy.edu.um.pa.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.um.pa.catalogo.Juego;

/** API REST del catalogo; es la que usan las pruebas de integracion y el smoke test del pipeline. */
@RestController
@RequestMapping("/api")
public class ApiCatalogoController {

    private final ServicioAplicacion servicio;

    public ApiCatalogoController(ServicioAplicacion servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/juegos")
    public List<Juego> listar(@RequestParam(name = "genero", required = false) String genero) {
        return servicio.buscar(genero);
    }

    @PostMapping("/juegos")
    public ResponseEntity<Juego> registrar(@Valid @RequestBody JuegoForm form) {
        Juego creado = servicio.registrar(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @DeleteMapping("/juegos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        return servicio.eliminar(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/generos")
    public List<String> generos() {
        return servicio.generosDisponibles();
    }

    @GetMapping("/estadisticas")
    public Map<String, Long> estadisticas() {
        return servicio.estadisticas();
    }
}
