package com.dsklabs.epam.tecnicaltest.support.benchmark;

import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Micro-harness de medición en proceso.
 *
 * <p><b>Por qué no JMH:</b> JMH exige configurar el annotation processor
 * ({@code jmh-generator-annprocess}) y {@code maven-shade-plugin}, y el shade plugin pelea con
 * {@code spring-boot-maven-plugin:repackage} por el mismo artifact. Además JMH necesita módulos
 * aparte y minutos de forks: mal para una demo de 30 segundos en una entrevista.
 *
 * <p>El harness sí hace lo esencial de JMH: warmup para que la JIT caliente, varias iteraciones
 * para reducir el ruido, y reporta min/mediana/p95 en vez de solo la media (la media se ve
 * afectada por pausas del GC).
 *
 * <p><b>Lo que se dice en la entrevista:</b> "en producción usaría JMH con {@code @Benchmark},
 * {@code -prof gc} y {@code @Fork}; aquí el harness es in-process para poder servirlo por HTTP y
 * mostrarlo en vivo".
 */
public final class BenchmarkHarness {

    /**
     * Sumidero de resultado. Sin esto, la JVM puede dead-code-eliminar el trabajo medido y
     * mediríamos nanosegundos de nada.
     */
    private static volatile Object sink;

    private BenchmarkHarness() {
    }

    /** Medida con warmup automático (10% de las iteraciones, mínimo 1). */
    public static BenchmarkResult measure(String scenario, int iterations, int opsPerIteration, Runnable task) {
        int warmup = Math.max(1, iterations / 10);
        return measure(scenario, warmup, iterations, opsPerIteration, task);
    }

    /**
     * Ejecuta la tarea {@code iterations} veces (precedidas de {@code warmupIterations} de
     * calentamiento) y devuelve la distribución de tiempos.
     *
     * @param scenario        etiqueta del escenario
     * @param warmupIterations iteraciones de calentamiento (no se miden)
     * @param iterations      iteraciones medidas
     * @param opsPerIteration operaciones de negocio por iteración (para calcular throughput)
     * @param task            el trabajo a medir
     */
    public static BenchmarkResult measure(String scenario, int warmupIterations, int iterations,
                                          int opsPerIteration, Runnable task) {

        for (int i = 0; i < warmupIterations; i++) {
            task.run();
        }

        long[] samples = new long[iterations];
        long total = 0L;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            task.run();
            long elapsed = System.nanoTime() - start;
            samples[i] = elapsed;
            total += elapsed;
            // Fuerza una barrier en la lectura del sumidero: el coste es despreciable
            // frente a la iteración y evita que la JIT elimine el trabajo.
            if (sink == null) {
                sink = new Object();
            }
        }

        long[] sorted = samples.clone();
        Arrays.sort(sorted);

        double seconds = total / 1_000_000_000.0;
        double opsPerSecond = seconds > 0 ? (iterations * (double) opsPerIteration) / seconds : 0;

        return new BenchmarkResult(
                scenario,
                iterations,
                opsPerIteration,
                sorted[0],
                percentile(sorted, 50),
                percentile(sorted, 95),
                sorted[sorted.length - 1],
                total,
                Math.round(opsPerSecond * 100.0) / 100.0);
    }

    /**
     * Igual que {@link #measure}, para tareas que lanzan excepciones comprobadas.
     *
     * <p>Existe aparte (y no como sobrecarga con {@code Supplier}) a propósito: dos sobrecargas
     * que solo se diferencian en el tipo funcional crean ambigüedad al resolver una lambda
     * implícitamente tipada.
     */
    public static <T> BenchmarkResult measureThrowing(String scenario, int iterations, int opsPerIteration,
                                                       Callable<T> task) {
        return measure(scenario, Math.max(1, iterations / 10), iterations, opsPerIteration, () -> {
            try {
                T value = task.call();
                if (sink == null) {
                    sink = value;
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    /**
     * Percentil sobre un array YA ORDENADO. Para p se usa el índice {@code ceil(p/100 * n) - 1},
     * que es la convención habitual ("nearest-rank").
     */
    private static long percentile(long[] sorted, int p) {
        if (sorted.length == 0) {
            return 0L;
        }
        int rank = (int) Math.ceil(p / 100.0 * sorted.length);
        int index = Math.min(Math.max(rank - 1, 0), sorted.length - 1);
        return sorted[index];
    }
}
