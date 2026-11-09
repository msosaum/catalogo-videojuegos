package uy.edu.um.pa.catalogo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Doble de prueba: implementacion en memoria del repositorio, sin tocar el disco. */
public class RepositorioEnMemoria implements RepositorioJuegos {

    private final Map<String, Juego> datos = new LinkedHashMap<>();

    @Override
    public List<Juego> listarTodos() {
        return new ArrayList<>(datos.values());
    }

    @Override
    public Optional<Juego> buscarPorId(String id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public Juego guardar(Juego juego) {
        datos.put(juego.getId(), juego);
        return juego;
    }

    @Override
    public boolean eliminar(String id) {
        return datos.remove(id) != null;
    }

    @Override
    public void vaciar() {
        datos.clear();
    }
}
