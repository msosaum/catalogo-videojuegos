package uy.edu.um.pa.web;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uy.edu.um.pa.catalogo.ServicioCatalogo;

/**
 * Carga unos pocos juegos de ejemplo la primera vez que arranca un ambiente vacio,
 * para que la demo no empiece con una pantalla en blanco. No se activa en pruebas.
 */
@Configuration
@Profile("!test")
public class DatosIniciales {

    @Bean
    public CommandLineRunner cargarEjemplos(ServicioCatalogo catalogo) {
        return args -> {
            if (catalogo.cantidad() > 0) {
                return;
            }
            catalogo.registrar("The Legend of Zelda: Breath of the Wild", "Aventura", 2017,
                    "https://upload.wikimedia.org/wikipedia/en/c/c6/The_Legend_of_Zelda_Breath_of_the_Wild.jpg");
            catalogo.registrar("Hollow Knight", "Aventura", 2017,
                    "https://upload.wikimedia.org/wikipedia/en/d/de/Hollow_Knight_2026_cover_art.jpg");
            catalogo.registrar("Age of Empires II", "Estrategia", 1999,
                    "https://upload.wikimedia.org/wikipedia/en/5/56/Age_of_Empires_II_-_The_Age_of_Kings_Coverart.png");
            catalogo.registrar("Final Fantasy VII", "RPG", 1997,
                    "https://upload.wikimedia.org/wikipedia/en/c/c2/Final_Fantasy_VII_Box_Art.jpg");
            catalogo.registrar("DOOM", "Accion", 1993,
                    "https://upload.wikimedia.org/wikipedia/en/5/57/Doom_cover_art.jpg");
        };
    }
}
