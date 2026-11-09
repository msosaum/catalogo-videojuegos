package uy.edu.um.pa.clasificacion;

/** Se lanza cuando se intenta clasificar un juego con un genero que no existe. */
public class GeneroDesconocidoException extends RuntimeException {

    public GeneroDesconocidoException(String genero) {
        super("Genero desconocido: " + genero);
    }
}
