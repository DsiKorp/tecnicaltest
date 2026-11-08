package com.dsklabs.epam.tecnicaltest.q01collections;

import com.dsklabs.epam.tecnicaltest.support.benchmark.BenchmarkHarness;
import com.dsklabs.epam.tecnicaltest.support.benchmark.BenchmarkResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

/**
 * Compara nuestras implementaciones (y las del JDK) en los cuatro escenarios que mandan en una
 * entrevista: añadir, acceso aleatorio, eliminar por delante y eliminar por detrás.
 *
 * <p>El resultado esperado, y el que hay que saber explicar:
 *
 * <table border="1">
 *   <caption>Complejidad</caption>
 *   <tr><th>Operación</th><th>Array (nuestro SimpleArrayList)</th><th>Lista enlazada (nuestro SimpleLinkedList)</th></tr>
 *   <tr><td>add al final</td><td>amortizado O(1)</td><td>O(1)</td></tr>
 *   <tr><td>get(i)</td><td>O(1)</td><td>O(n)</td></tr>
 *   <tr><td>remove(0)</td><td>O(n) — desplaza todo</td><td>O(1) — re-liga punteros</td></tr>
 *   <tr><td>remove(último)</td><td>O(1)</td><td>O(1) (con puntero tail)</td></tr>
 * </table>
 *
 * <p>Es decir: <b>la lista enlazada gana en las operaciones que tocan los extremos y pierde
 * en el acceso por índice</b>. Por eso en un JDK real, {@code ArrayList} es la opción por
 * defecto y {@code LinkedList} casi nunca se usa (en el JDK 21+, {@code SequencedCollection}
 * cubre bien el caso de cola).
 */
@Service
public class CollectionsDemoService {

    /**
     * Ejecuta la batería completa de benchmarks.
     *
     * @param size      número de elementos de la lista
     * @param iterations repeticiones por escenario
     * @return resultados por escenario e implementación
     */
    public List<ScenarioResult> benchmark(int size, int iterations) {
        int safeSize = Math.max(100, Math.min(size, 200_000));
        int safeIterations = Math.max(1, Math.min(iterations, 2_000));

        List<ScenarioResult> results = new ArrayList<>();

        results.add(new ScenarioResult("append (añadir al final)", "O(1) amortizado",
                benchmarkAppend(safeSize, safeIterations)));
        results.add(new ScenarioResult("get(index) aleatorio", "Array O(1) vs LinkedList O(n)",
                benchmarkRandomAccess(safeSize, safeIterations)));
        results.add(new ScenarioResult("remove(0) (eliminar por delante)", "Array O(n) vs LinkedList O(1)",
                benchmarkRemoveFront(safeSize, safeIterations)));
        results.add(new ScenarioResult("remove(último) (eliminar por detrás)", "O(1) en ambos",
                benchmarkRemoveBack(safeSize, safeIterations)));

        return results;
    }

    // ---------------------------------------------------------------- scenarios

    private Map<String, BenchmarkResult> benchmarkAppend(int size, int iterations) {
        Map<String, BenchmarkResult> results = new LinkedHashMap<>();
        results.put("SimpleArrayList (nuestro)",
                BenchmarkHarness.measure("append/SimpleArrayList", iterations, size, () -> {
                    SimpleArrayList<Integer> list = new SimpleArrayList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                }));
        results.put("SimpleLinkedList (nuestro)",
                BenchmarkHarness.measure("append/SimpleLinkedList", iterations, size, () -> {
                    SimpleLinkedList<Integer> list = new SimpleLinkedList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                }));
        results.put("java.util.ArrayList (JDK)",
                BenchmarkHarness.measure("append/ArrayList", iterations, size, () -> {
                    List<Integer> list = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                }));
        results.put("java.util.LinkedList (JDK)",
                BenchmarkHarness.measure("append/LinkedList", iterations, size, () -> {
                    List<Integer> list = new LinkedList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                }));
        return results;
    }

    private Map<String, BenchmarkResult> benchmarkRandomAccess(int size, int iterations) {
        Random random = new Random(42);
        int[] indexes = new int[Math.min(size, 1_000)];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = random.nextInt(size);
        }

        Map<String, BenchmarkResult> results = new LinkedHashMap<>();
        results.put("SimpleArrayList (nuestro)",
                BenchmarkHarness.measure("get/SimpleArrayList", iterations, indexes.length, () -> {
                    SimpleArrayList<Integer> list = new SimpleArrayList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                    int sum = 0;
                    for (int index : indexes) {
                        sum += list.get(index);
                    }
                    if (sum == Integer.MIN_VALUE) {
                        throw new IllegalStateException("imposible");
                    }
                }));
        results.put("SimpleLinkedList (nuestro)",
                BenchmarkHarness.measure("get/SimpleLinkedList", iterations, indexes.length, () -> {
                    SimpleLinkedList<Integer> list = new SimpleLinkedList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                    int sum = 0;
                    for (int index : indexes) {
                        sum += list.get(index);
                    }
                    if (sum == Integer.MIN_VALUE) {
                        throw new IllegalStateException("imposible");
                    }
                }));
        results.put("java.util.ArrayList (JDK)",
                BenchmarkHarness.measure("get/ArrayList", iterations, indexes.length, () -> {
                    List<Integer> list = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                    int sum = 0;
                    for (int index : indexes) {
                        sum += list.get(index);
                    }
                    if (sum == Integer.MIN_VALUE) {
                        throw new IllegalStateException("imposible");
                    }
                }));
        results.put("java.util.LinkedList (JDK)",
                BenchmarkHarness.measure("get/LinkedList", iterations, indexes.length, () -> {
                    List<Integer> list = new LinkedList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                    int sum = 0;
                    for (int index : indexes) {
                        sum += list.get(index);
                    }
                    if (sum == Integer.MIN_VALUE) {
                        throw new IllegalStateException("imposible");
                    }
                }));
        return results;
    }

    private Map<String, BenchmarkResult> benchmarkRemoveFront(int size, int iterations) {
        Map<String, BenchmarkResult> results = new LinkedHashMap<>();
        results.put("SimpleArrayList (nuestro)",
                BenchmarkHarness.measure("removeFront/SimpleArrayList", iterations, size / 2, () -> {
                    SimpleArrayList<Integer> list = new SimpleArrayList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                    for (int i = 0; i < size / 2; i++) {
                        list.remove(0);
                    }
                }));
        results.put("SimpleLinkedList (nuestro)",
                BenchmarkHarness.measure("removeFront/SimpleLinkedList", iterations, size / 2, () -> {
                    SimpleLinkedList<Integer> list = new SimpleLinkedList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                    for (int i = 0; i < size / 2; i++) {
                        list.remove(0);
                    }
                }));
        results.put("java.util.ArrayList (JDK)",
                BenchmarkHarness.measure("removeFront/ArrayList", iterations, size / 2, () -> {
                    List<Integer> list = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                    for (int i = 0; i < size / 2; i++) {
                        list.remove(0);
                    }
                }));
        results.put("java.util.LinkedList (JDK)",
                BenchmarkHarness.measure("removeFront/LinkedList", iterations, size / 2, () -> {
                    List<Integer> list = new LinkedList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                    for (int i = 0; i < size / 2; i++) {
                        list.remove(0);
                    }
                }));
        return results;
    }

    private Map<String, BenchmarkResult> benchmarkRemoveBack(int size, int iterations) {
        Map<String, BenchmarkResult> results = new LinkedHashMap<>();
        results.put("SimpleArrayList (nuestro)",
                BenchmarkHarness.measure("removeBack/SimpleArrayList", iterations, size / 2, () -> {
                    SimpleArrayList<Integer> list = new SimpleArrayList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                    for (int i = 0; i < size / 2; i++) {
                        list.remove(list.size() - 1);
                    }
                }));
        results.put("SimpleLinkedList (nuestro)",
                BenchmarkHarness.measure("removeBack/SimpleLinkedList", iterations, size / 2, () -> {
                    SimpleLinkedList<Integer> list = new SimpleLinkedList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                    for (int i = 0; i < size / 2; i++) {
                        list.remove(list.size() - 1);
                    }
                }));
        results.put("java.util.ArrayList (JDK)",
                BenchmarkHarness.measure("removeBack/ArrayList", iterations, size / 2, () -> {
                    List<Integer> list = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                    for (int i = 0; i < size / 2; i++) {
                        list.remove(list.size() - 1);
                    }
                }));
        results.put("java.util.LinkedList (JDK)",
                BenchmarkHarness.measure("removeBack/LinkedList", iterations, size / 2, () -> {
                    List<Integer> list = new LinkedList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(i);
                    }
                    for (int i = 0; i < size / 2; i++) {
                        list.remove(list.size() - 1);
                    }
                }));
        return results;
    }

    /**
     * Estructura interna de la respuesta.
     *
     * @param escenario nombre del escenario
     * @param complejidad qué dice la teoría
     * @param resultados una entrada por implementación
     */
    public record ScenarioResult(String escenario, String complejidad, Map<String, BenchmarkResult> resultados) {
    }
}
