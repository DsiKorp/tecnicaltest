package com.dsklabs.epam.tecnicaltest.q05hashset;

import com.dsklabs.epam.tecnicaltest.support.web.ApiResponse;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la pregunta 5 ("¿cómo se ordena un HashSet de enteros?").
 *
 * <ul>
 *   <li>{@code GET /q05/order?strategy=tree&size=20} — ordena el conjunto con la estrategia
 *       indicada y devuelve <b>junto</b> el resultado y el orden de iteración original, para que
 *       se vea la diferencia de un vistazo.</li>
 *   <li>{@code GET /q05/explain?size=20} — el desglose hash → cubo de cada elemento.</li>
 *   <li>{@code GET /q05/collisions} — la colisión clásica de {@code "Ea"} y {@code "FB"}.</li>
 *   <li>{@code GET /q05/treeification} — los 8 elementos que caen en el mismo cubo.</li>
 * </ul>
 *
 * <p><b>Por qué el conjunto de la demo no es 1, 2, 3...</b> Con los enteros consecutivos
 * {@code 1..N} el orden de iteración de un {@code HashSet} <b>coincide con el orden natural</b>,
 * porque el índice de cubo es {@code valor & (tableSize - 1)} y, mientras la tabla sea mayor que
 * el mayor valor, sale el valor tal cual. Es una <b>casualidad del hash</b>, no una garantía: en
 * cuanto los valores no son consecutivos, el orden se desordena. Para que la diferencia entre el
 * orden de iteración y el ordenado se vea de verdad (que es el objetivo de la pregunta), la demo
 * usa valores separados por un paso ({@code 1, 1+paso, 1+2*paso, ...}).
 */
@RestController
@RequestMapping("/q05")
public class HashSetController {

    /** Tope de elementos para que la demo no se haga enorme. */
    private static final int MAX_SIZE = 500;

    /** Paso por defecto entre los valores del conjunto de demostración. */
    private static final int DEFAULT_STRIDE = 37;

    private final HashSetOrderingService service;

    public HashSetController(HashSetOrderingService service) {
        this.service = service;
    }

    /**
     * Ordena el conjunto con la estrategia pedida y enseña las dos órdenes.
     */
    @GetMapping("/order")
    public ApiResponse<Map<String, Object>> order(
            @RequestParam(required = false) String strategy,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "37") int stride) {

        int safeSize = Math.max(1, Math.min(size, MAX_SIZE));
        int safeStride = Math.max(1, stride);

        Set<Integer> set = demoSet(safeSize, safeStride);

        SortStrategy selected = SortStrategy.from(strategy);
        String note = null;
        if (selected == null) {
            selected = SortStrategy.TREE;
            note = strategy == null || strategy.isBlank()
                    ? "No se indicó estrategia: se usa TREE por defecto."
                    : "Estrategia '" + strategy + "' no reconocida: se usa TREE por defecto. "
                            + "Valores válidos: tree, linked, list, stream.";
        }

        List<Integer> originalOrder = List.copyOf(set);
        List<Integer> sorted = service.sort(set, selected);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("estrategia", selected.name());
        data.put("estrategiaLegible", selected.displayName());
        data.put("descripcionDeLaEstrategia", selected.description());
        data.put("cantidadDeElementos", safeSize);
        data.put("pasoEntreValores", safeStride);
        data.put("valoresDelConjunto", originalOrder);
        data.put("ordenDeIteracionOriginal", originalOrder);
        data.put("ordenOrdenado", sorted);
        data.put("coincidenLosOrdenes", originalOrder.equals(sorted));
        data.put("nota", note);
        data.put("explicacion",
                "El orden de iteración de un HashSet es el orden de los cubos de su tabla hash: "
                        + "ni el de inserción ni el natural (ordenado). Para ordenarlo hace falta "
                        + "una estructura extra (TreeSet, LinkedHashSet) o copiar a una List y "
                        + "ordenarla. Compárese 'ordenDeIteracionOriginal' con 'ordenOrdenado'. "
                        + "OJO con un detalle muy típico de entrevista: con los enteros "
                        + "consecutivos 1..N el orden de iteración SÍ coincide con el natural "
                        + "(porque el cubo es el propio valor mientras la tabla sea mayor que N). "
                        + "Es una casualidad del hash, no una garantía: por eso esta demo usa "
                        + "valores no consecutivos.");

        return ApiResponse.of("q05-hashset",
                "Ordenar un HashSet: el orden original (por cubos) frente al orden de la estrategia " + selected.name(),
                data);
    }

    /**
     * Explica, elemento a elemento, por qué el conjunto itera en el orden en que itera.
     */
    @GetMapping("/explain")
    public ApiResponse<Map<String, Object>> explain(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "37") int stride) {

        int safeSize = Math.max(1, Math.min(size, MAX_SIZE));
        int safeStride = Math.max(1, stride);

        return ApiResponse.of("q05-hashset",
                "HashSet por dentro: hashCode disperso y cubo de cada elemento",
                service.explain(demoSet(safeSize, safeStride)));
    }

    /**
     * La colisión clásica: {@code "Ea"} y {@code "FB"} comparten hashCode.
     */
    @GetMapping("/collisions")
    public ApiResponse<Map<String, Object>> collisions() {
        return ApiResponse.of("q05-hashset",
                "Colisión de hash: \"Ea\" y \"FB\" tienen el mismo hashCode y el mismo cubo",
                service.collisions());
    }

    /**
     * La treeificación: 8 elementos en el mismo cubo.
     */
    @GetMapping("/treeification")
    public ApiResponse<Map<String, Object>> treeification() {
        return ApiResponse.of("q05-hashset",
                "Treeificación: 8 elementos en el mismo cubo disparan el árbol rojo-negro",
                service.treeification());
    }

    /**
     * Construye el conjunto de demostración: {@code size} valores separados por {@code stride}.
     *
     * <p>Con {@code stride = 1} se obtendrían los enteros consecutivos 1..N, cuyo orden de
     * iteración en un {@code HashSet} coincide con el natural (casualidad del hash). Con un paso
     * mayor, el reparto por cubos desordena los valores y la diferencia se ve.
     */
    private Set<Integer> demoSet(int size, int stride) {
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < size; i++) {
            set.add(1 + i * stride);
        }
        return set;
    }
}
