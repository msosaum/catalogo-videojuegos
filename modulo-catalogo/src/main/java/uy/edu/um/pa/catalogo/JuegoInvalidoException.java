package uy.edu.um.pa.catalogo;

/** Se lanza cuando los datos de un juego no cumplen las reglas del catalogo. */
public class JuegoInvalidoException extends RuntimeException {

    public JuegoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
