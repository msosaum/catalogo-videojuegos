package uy.edu.um.pa.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uy.edu.um.pa.catalogo.JuegoInvalidoException;
import uy.edu.um.pa.clasificacion.GeneroDesconocidoException;

/** Traduce las excepciones de los modulos a respuestas HTTP 400 con un mensaje legible. */
@RestControllerAdvice(assignableTypes = ApiCatalogoController.class)
public class ManejadorErroresApi {

    @ExceptionHandler({JuegoInvalidoException.class, GeneroDesconocidoException.class})
    public ResponseEntity<Map<String, String>> datosInvalidos(RuntimeException e) {
        return respuesta(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Datos invalidos");
        return respuesta(mensaje);
    }

    private ResponseEntity<Map<String, String>> respuesta(String mensaje) {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }
}
