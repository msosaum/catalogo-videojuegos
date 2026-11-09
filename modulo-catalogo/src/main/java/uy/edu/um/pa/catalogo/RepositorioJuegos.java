package uy.edu.um.pa.catalogo;

import java.util.List;
import java.util.Optional;

/** Contrato de persistencia del catalogo, independiente del medio de almacenamiento. */
public interface RepositorioJuegos {

    List<Juego> listarTodos();

    Optional<Juego> buscarPorId(String id);

    Juego guardar(Juego juego);

    boolean eliminar(String id);

    void vaciar();
}
