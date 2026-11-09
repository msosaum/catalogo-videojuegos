package uy.edu.um.pa.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicacion web.
 * Escanea todo el paquete uy.edu.um.pa para levantar los beans de los dos modulos.
 */
@SpringBootApplication(scanBasePackages = "uy.edu.um.pa")
public class AplicacionCatalogo {

    public static void main(String[] args) {
        SpringApplication.run(AplicacionCatalogo.class, args);
    }
}
