package com.dsklabs.epam.tecnicaltest.q01collections;

import com.dsklabs.epam.tecnicaltest.support.web.ApiResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la pregunta 1 (ArrayList vs LinkedList).
 *
 * <ul>
 *   <li>{@code GET /q01/benchmark} — la batería de escenarios con los números.</li>
 *   <li>{@code GET /q01/internals} — el estado interno de las dos estructuras (capacidad,
 *       número de elementos, cadena de nodos): sirve para enseñar el "por qué".</li>
 * </ul>
 */
@RestController
@RequestMapping("/q01")
public class CollectionsDemoController {

    private final CollectionsDemoService service;

    public CollectionsDemoController(CollectionsDemoService service) {
        this.service = service;
    }

    @GetMapping("/benchmark")
    public ApiResponse<List<CollectionsDemoService.ScenarioResult>> benchmark(
            @RequestParam(defaultValue = "5000") int size,
            @RequestParam(defaultValue = "20") int iterations) {

        List<CollectionsDemoService.ScenarioResult> results = service.benchmark(size, iterations);

        return ApiResponse.of(
                "q01-collections",
                "ArrayList (array) vs LinkedList (cadena de nodos): los números de cada escenario",
                results);
    }

    @GetMapping("/internals")
    public ApiResponse<Map<String, Object>> internals(@RequestParam(defaultValue = "5") int elements) {
        SimpleArrayList<Integer> arrayList = new SimpleArrayList<>();
        SimpleLinkedList<Integer> linkedList = new SimpleLinkedList<>();
        IntArrayList intArrayList = new IntArrayList();

        for (int i = 0; i < elements; i++) {
            arrayList.add(i);
            linkedList.add(i);
            intArrayList.add(i);
        }

        Map<String, Object> arrayInternals = new LinkedHashMap<>();
        arrayInternals.put("estructura", "Object[] de backing + int size");
        arrayInternals.put("capacidad", arrayList.capacity());
        arrayInternals.put("elementos", arrayList.size());
        arrayInternals.put("accesoPorIndice", "O(1) — es aritmética sobre el índice del array");
        arrayInternals.put("insertarEnMedio", "O(n) — System.arraycopy desplaza los elementos");

        Map<String, Object> linkedInternals = new LinkedHashMap<>();
        linkedInternals.put("estructura", "cadena de nodos {valor, next, prev} + punteros head/tail");
        linkedInternals.put("capacidad", "no aplica (crece por nodos)");
        linkedInternals.put("elementos", linkedList.size());
        linkedInternals.put("accesoPorIndice", "O(n) — hay que recorrer la cadena");
        linkedInternals.put("insertarEnMedio", "O(1) — se re-ligan los punteros vecinos");
        linkedInternals.put("extra", "implementa Deque: push/pop/offer/poll");

        Map<String, Object> primitives = new LinkedHashMap<>();
        primitives.put("estructura", "int[] puro, sin boxing");
        primitives.put("capacidad", intArrayList.capacity());
        primitives.put("elementos", intArrayList.size());
        primitives.put("suma", intArrayList.sum());
        primitives.put("nota", "Un Integer ocupa ~16 bytes de objeto; un int en un array, 4 bytes.");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("SimpleArrayList", arrayInternals);
        data.put("SimpleLinkedList", linkedInternals);
        data.put("IntArrayList", primitives);
        data.put("conclusion",
                "ArrayList para acceso por índice y recorrido; LinkedList solo cuando el acceso es "
                        + "secuencial o se manipulan los extremos (Deque). En la práctica, ArrayList "
                        + "casi siempre es la respuesta correcta.");

        return ApiResponse.of("q01-internals", "Estructura interna de las dos implementaciones", data);
    }
}
