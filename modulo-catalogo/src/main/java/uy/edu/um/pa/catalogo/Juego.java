package uy.edu.um.pa.catalogo;

import java.util.Objects;

/**
 * Entidad del dominio: un videojuego del catalogo.
 * Es un POJO serializable a JSON (Jackson) para la persistencia en archivo.
 */
public class Juego {

    private String id;
    private String titulo;
    private String genero;
    private int anio;
    private String imagenUrl;

    public Juego() {
        // constructor vacio requerido por Jackson
    }

    public Juego(String id, String titulo, String genero, int anio, String imagenUrl) {
        this.id = id;
        this.titulo = titulo;
        this.genero = genero;
        this.anio = anio;
        this.imagenUrl = imagenUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public int getAnio() {
        return anio;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Juego otro)) {
            return false;
        }
        return Objects.equals(id, otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Juego{" + titulo + " (" + anio + ") - " + genero + "}";
    }
}
