package uy.edu.um.pa.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uy.edu.um.pa.catalogo.JuegoInvalidoException;
import uy.edu.um.pa.clasificacion.GeneroDesconocidoException;

/** Interfaz web (Thymeleaf) del catalogo. */
@Controller
public class CatalogoController {

    private final ServicioAplicacion servicio;
    private final String ambiente;
    private final String version;

    public CatalogoController(ServicioAplicacion servicio,
                              @Value("${app.ambiente:DESARROLLO}") String ambiente,
                              @Value("${app.version:dev}") String version) {
        this.servicio = servicio;
        this.ambiente = ambiente;
        this.version = version;
    }

    @GetMapping("/")
    public String inicio(@RequestParam(name = "genero", required = false) String genero, Model model) {
        model.addAttribute("juegos", servicio.buscar(genero));
        model.addAttribute("generos", servicio.generosDisponibles());
        model.addAttribute("generoSeleccionado", genero == null ? "" : genero);
        model.addAttribute("estadisticas", servicio.estadisticas());
        model.addAttribute("total", servicio.cantidad());
        model.addAttribute("ambiente", ambiente);
        model.addAttribute("version", version);
        if (!model.containsAttribute("juegoForm")) {
            model.addAttribute("juegoForm", new JuegoForm());
        }
        return "index";
    }

    @PostMapping("/juegos")
    public String registrar(@ModelAttribute JuegoForm juegoForm, RedirectAttributes redirect) {
        try {
            servicio.registrar(juegoForm);
            redirect.addFlashAttribute("mensaje", "Juego registrado: " + juegoForm.getTitulo());
        } catch (GeneroDesconocidoException | JuegoInvalidoException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/juegos/{id}/eliminar")
    public String eliminar(@PathVariable String id, RedirectAttributes redirect) {
        if (servicio.eliminar(id)) {
            redirect.addFlashAttribute("mensaje", "Juego eliminado del catalogo");
        } else {
            redirect.addFlashAttribute("error", "No existe un juego con ese identificador");
        }
        return "redirect:/";
    }
}
