package uy.edu.um.pa.catalogo;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Modulo 1 del sistema: reglas de negocio del catalogo (alta, baja y consulta de juegos).
 * No conoce al modulo de clasificacion; la dependencia va en el sentido contrario.
 */
@Service
public class ServicioCatalogo {

    /** Primer videojuego comercial de la historia; no se aceptan anios anteriores. */
    public static final int ANIO_MINIMO = 1958;

    private final RepositorioJuegos repositorio;

    public ServicioCatalogo(RepositorioJuegos repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Registra un juego nuevo validando los datos obligatorios.
     *
     * @throws JuegoInvalidoException si el titulo esta vacio, el anio esta fuera de rango
     *                                o ya existe un juego con el mismo titulo.
     */
    public Juego registrar(String titulo, String genero, int anio, String imagenUrl) {
        String tituloNormalizado = titulo == null ? "" : titulo.trim();
        if (tituloNormalizado.isEmpty()) {
            throw new JuegoInvalidoException("El titulo del juego es obligatorio");
        }
        if (genero == null || genero.isBlank()) {
            throw new JuegoInvalidoException("El genero del juego es obligatorio");
        }
        int anioActual = Year.now().getValue();
        if (anio < ANIO_MINIMO || anio > anioActual + 5) {
            throw new JuegoInvalidoException(
                    "El anio debe estar entre " + ANIO_MINIMO + " y " + (anioActual + 5) + ", se recibio " + anio);
        }
        if (existeTitulo(tituloNormalizado)) {
            throw new JuegoInvalidoException("Ya existe un juego registrado con el titulo: " + tituloNormalizado);
        }
        Juego juego = new Juego(UUID.randomUUID().toString(), tituloNormalizado, genero.trim(), anio,
                imagenUrl == null || imagenUrl.isBlank() ? null : imagenUrl.trim());
        return repositorio.guardar(juego);
    }

    public boolean existeTitulo(String titulo) {
        return repositorio.listarTodos().stream()
                .anyMatch(j -> j.getTitulo().equalsIgnoreCase(titulo.trim()));
    }

    public List<Juego> listar() {
        return repositorio.listarTodos();
    }

    public Optional<Juego> buscarPorId(String id) {
        return repositorio.buscarPorId(id);
    }

    public boolean eliminar(String id) {
        return repositorio.eliminar(id);
    }

    public int cantidad() {
        return repositorio.listarTodos().size();
    }
}
