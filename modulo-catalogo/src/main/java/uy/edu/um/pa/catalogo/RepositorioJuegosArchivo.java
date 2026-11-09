package uy.edu.um.pa.catalogo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * Persistencia del catalogo en un archivo JSON.
 *
 * <p>Se eligio un archivo de texto (permitido por la letra del entregable) para que el
 * ambiente de produccion simulado no dependa de un motor de base de datos: basta con
 * montar un volumen de Docker sobre el directorio de datos para que la informacion
 * sobreviva a cada despliegue.</p>
 */
@Repository
public class RepositorioJuegosArchivo implements RepositorioJuegos {

    private final Path archivo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Juego> juegos = new LinkedHashMap<>();

    public RepositorioJuegosArchivo(@Value("${catalogo.archivo:datos/juegos.json}") String rutaArchivo) {
        this.archivo = Path.of(rutaArchivo);
        cargar();
    }

    private synchronized void cargar() {
        juegos.clear();
        if (!Files.exists(archivo)) {
            return;
        }
        try {
            String contenido = Files.readString(archivo).trim();
            if (contenido.isEmpty()) {
                return;
            }
            List<Juego> leidos = mapper.readValue(contenido, new TypeReference<List<Juego>>() { });
            for (Juego juego : leidos) {
                juegos.put(juego.getId(), juego);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo de catalogo: " + archivo, e);
        }
    }

    private synchronized void persistir() {
        try {
            Path padre = archivo.toAbsolutePath().getParent();
            if (padre != null) {
                Files.createDirectories(padre);
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(archivo.toFile(), new ArrayList<>(juegos.values()));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir el archivo de catalogo: " + archivo, e);
        }
    }

    @Override
    public synchronized List<Juego> listarTodos() {
        return Collections.unmodifiableList(new ArrayList<>(juegos.values()));
    }

    @Override
    public synchronized Optional<Juego> buscarPorId(String id) {
        return Optional.ofNullable(juegos.get(id));
    }

    @Override
    public synchronized Juego guardar(Juego juego) {
        juegos.put(juego.getId(), juego);
        persistir();
        return juego;
    }

    @Override
    public synchronized boolean eliminar(String id) {
        boolean existia = juegos.remove(id) != null;
        if (existia) {
            persistir();
        }
        return existia;
    }

    @Override
    public synchronized void vaciar() {
        juegos.clear();
        persistir();
    }
}
