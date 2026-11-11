package uy.edu.um.pa.clasificacion;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Optional;

/** Generos admitidos por el catalogo. */
public enum Genero {

    ACCION("Accion"),
    AVENTURA("Aventura"),
    RPG("RPG"),
    ESTRATEGIA("Estrategia"),
    DEPORTES("Deportes"),
    SIMULACION("Simulacion"),
    PUZZLE("Puzzle"),
    // Demo Caso 1: descomentar esta linea, commitear y pushear.
    PLATAFORMAS("Plataformas"),
    TERROR("Terror");

    private final String etiqueta;

    Genero(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * Resuelve un genero a partir de texto libre, ignorando mayusculas, espacios y tildes.
     * Devuelve vacio si el texto no corresponde a ningun genero conocido.
     */
    public static Optional<Genero> desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String normalizado = normalizar(texto);
        return Arrays.stream(values())
                .filter(g -> normalizar(g.name()).equals(normalizado)
                        || normalizar(g.etiqueta).equals(normalizado))
                .findFirst();
    }

    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.toUpperCase();
    }
}
