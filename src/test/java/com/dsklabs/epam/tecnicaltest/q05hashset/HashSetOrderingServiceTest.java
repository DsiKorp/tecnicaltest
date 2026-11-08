package com.dsklabs.epam.tecnicaltest.q05hashset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Capa UNIT de la pregunta 5. Sin Spring: es lógica pura y tiene que correr en milisegundos.
 */
class HashSetOrderingServiceTest {

    private final HashSetOrderingService service = new HashSetOrderingService();

    /**
     * Conjunto de demostración: valores NO consecutivos.
     *
     * <p>Hace falta porque con los enteros consecutivos 1..N el orden de iteración de un
     * {@code HashSet} coincide con el natural (el índice de cubo es el propio valor), y esa
     * casualidad haría que la demo no enseñase nada.
     */
    private Set<Integer> demoSet(int size, int stride) {
        Set<Integer> set = new HashSet<>();
        for (int i = 0; i < size; i++) {
            set.add(1 + i * stride);
        }
        return set;
    }

    @Test
    @DisplayName("TREE ordena de forma ascendente (TreeSet, orden natural)")
    void treeSortsAscending() {
        List<Integer> sorted = service.sort(demoSet(20, 37), SortStrategy.TREE);

        assertThat(sorted).isSorted();
        assertThat(sorted).containsExactly(1, 38, 75, 112, 149, 186, 223, 260, 297, 334,
                371, 408, 445, 482, 519, 556, 593, 630, 667, 704);
    }

    @Test
    @DisplayName("LINKED conserva el orden de inserción (LinkedHashSet)")
    void linkedKeepsInsertionOrder() {
        Set<Integer> set = new LinkedHashSet<>();
        set.add(30);
        set.add(10);
        set.add(20);

        List<Integer> ordered = service.sort(set, SortStrategy.LINKED);

        assertThat(ordered).containsExactly(30, 10, 20);
    }

    @Test
    @DisplayName("LIST y STREAM también devuelven el orden ascendente")
    void listAndStreamSortAscending() {
        Set<Integer> set = demoSet(20, 37);

        assertThat(service.sort(set, SortStrategy.LIST)).isSorted();
        assertThat(service.sort(set, SortStrategy.STREAM)).isSorted();
        assertThat(service.sort(set, SortStrategy.LIST))
                .containsExactlyElementsOf(service.sort(set, SortStrategy.STREAM));
    }

    @Test
    @DisplayName("El orden de iteración del HashSet NO es el ordenado (pero es la misma permutación)")
    void hashSetIterationOrderIsNotSorted() {
        Set<Integer> set = demoSet(20, 37);

        List<Integer> iterationOrder = new ArrayList<>(set);
        List<Integer> sorted = service.sort(set, SortStrategy.TREE);

        // Mismos elementos...
        assertThat(iterationOrder).containsExactlyInAnyOrderElementsOf(sorted);
        assertThat(iterationOrder).hasSameSizeAs(sorted);
        // ...pero no el mismo orden: depende del hash, así que no se afirma un orden concreto.
        assertThat(iterationOrder).isNotEqualTo(sorted);
    }

    @Test
    @DisplayName("Con enteros consecutivos el orden de iteración coincide con el natural (casualidad)")
    void consecutiveIntegersCoincideWithSortedOrder() {
        Set<Integer> consecutive = new HashSet<>();
        for (int i = 1; i <= 20; i++) {
            consecutive.add(i);
        }

        List<Integer> iterationOrder = new ArrayList<>(consecutive);

        // El cubo de la clave i es (i ^ (i >>> 16)) & (tableSize - 1) == i mientras la tabla
        // sea mayor que el valor, así que el recorrido por cubos sale en orden natural.
        // Es una casualidad del hash, NO una garantía del Set: por eso la demo usa valores
        // no consecutivos.
        assertThat(iterationOrder).isSorted();
        assertThat(iterationOrder).containsExactlyElementsOf(service.sort(consecutive, SortStrategy.TREE));
    }

    @Test
    @DisplayName("explain reporta el hashCode y un índice de cubo dentro de [0, 16)")
    void explainReportsBucketIndex() {
        Map<String, Object> data = service.explain(demoSet(20, 37));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elements = (List<Map<String, Object>>) data.get("elementos");

        assertThat(elements).hasSize(20);
        assertThat(data.get("capacidadDeLaTabla")).isEqualTo(16);
        assertThat(data.get("factorDeCarga")).isEqualTo(0.75);

        for (Map<String, Object> element : elements) {
            int hash = (int) element.get("hashCode");
            int bucket = (int) element.get("indiceDeCubo");

            assertThat(hash).isEqualTo(((Integer) element.get("valor")).hashCode());
            assertThat(bucket).isBetween(0, 15);
            assertThat(bucket).isEqualTo((hash ^ (hash >>> 16)) & 15);
        }
    }

    @Test
    @DisplayName("collisions muestra que \"Ea\" y \"FB\" comparten hashCode 2236 y cubo")
    void collisionsShareHashCode() {
        Map<String, Object> data = service.collisions();

        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) data.get("primera");
        @SuppressWarnings("unchecked")
        Map<String, Object> second = (Map<String, Object>) data.get("segunda");

        assertThat(first.get("hashCode")).isEqualTo(2236);
        assertThat(second.get("hashCode")).isEqualTo(2236);
        assertThat(first.get("indiceDeCubo")).isEqualTo(second.get("indiceDeCubo"));
        assertThat(data.get("mismoHashCode")).isEqualTo(true);
        assertThat(data.get("igualesPorEquals")).isEqualTo(false);
    }

    @Test
    @DisplayName("treeification reporta los 8 elementos en el mismo cubo")
    void treeificationReportsEightInSameBucket() {
        Map<String, Object> data = service.treeification();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elements = (List<Map<String, Object>>) data.get("elementos");

        assertThat(elements).hasSize(8);
        assertThat(data.get("cantidadDeElementos")).isEqualTo(8);
        assertThat(data.get("umbralDeTreeificacion")).isEqualTo(8);

        Integer bucket = (Integer) elements.get(0).get("indiceDeCubo");
        assertThat(bucket).isEqualTo(1);
        for (Map<String, Object> element : elements) {
            assertThat(element.get("indiceDeCubo")).isEqualTo(bucket);
        }
    }
}
