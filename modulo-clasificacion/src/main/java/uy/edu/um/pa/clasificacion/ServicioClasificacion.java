package uy.edu.um.pa.clasificacion;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uy.edu.um.pa.catalogo.Juego;

/**
 * Modulo 2 del sistema: clasificacion y filtrado del catalogo por genero.
 * Depende del modulo de catalogo (usa su entidad {@link Juego}) pero no de la capa web.
 */
@Service
public class ServicioClasificacion {

    /**
     * Valida un genero escrito por el usuario y devuelve su etiqueta canonica.
     *
     * @throws GeneroDesconocidoException si el texto no corresponde a un genero conocido.
     */
   
    public String normalizar(String generoLibre) { return generoLibre; }

    public boolean esGeneroValido(String generoLibre) {
        return Genero.desde(generoLibre).isPresent();
    }

    /** Filtra los juegos por genero. Si el genero es nulo o vacio devuelve la lista completa. */
    public List<Juego> filtrarPorGenero(List<Juego> juegos, String generoLibre) {
        if (generoLibre == null || generoLibre.isBlank()) {
            return List.copyOf(juegos);
        }
        Optional<Genero> genero = Genero.desde(generoLibre);
        if (genero.isEmpty()) {
            return List.of();
        }
        return juegos.stream()
                .filter(j -> Genero.desde(j.getGenero()).orElse(null) == genero.get())
                .collect(Collectors.toList());
    }

    /** Cuenta cuantos juegos hay en cada genero presente en el catalogo, de mayor a menor. */
    public Map<String, Long> contarPorGenero(List<Juego> juegos) {
        return juegos.stream()
                .map(j -> Genero.desde(j.getGenero()).map(Genero::getEtiqueta).orElse("Sin clasificar"))
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /** Agrupa el catalogo por genero, ordenando cada grupo por anio descendente. */
    public Map<String, List<Juego>> agruparPorGenero(List<Juego> juegos) {
        Map<String, List<Juego>> agrupados = juegos.stream()
                .collect(Collectors.groupingBy(
                        j -> Genero.desde(j.getGenero()).map(Genero::getEtiqueta).orElse("Sin clasificar"),
                        LinkedHashMap::new,
                        Collectors.toList()));
        agrupados.values().forEach(lista -> lista.sort(Comparator.comparingInt(Juego::getAnio).reversed()));
        return agrupados;
    }

    /** Lista de etiquetas de todos los generos admitidos, para poblar el formulario web. */
    public List<String> generosDisponibles() {
        return java.util.Arrays.stream(Genero.values())
                .map(Genero::getEtiqueta)
                .collect(Collectors.toList());
    }
}
