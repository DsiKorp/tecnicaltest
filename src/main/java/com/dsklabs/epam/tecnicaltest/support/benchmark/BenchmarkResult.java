package com.dsklabs.epam.tecnicaltest.support.benchmark;

/**
 * Resultado de una medición de rendimiento.
 *
 * <p>Se expone tal cual por HTTP en los endpoints de demo (Q1, Q7, Q9) para que en la
 * entrevista se puedan ver los números sin abrir un profiler.
 *
 * @param scenario       nombre del escenario medido
 * @param iterations     cuántas iteraciones se ejecutaron (después del warmup)
 * @param opsPerIteration cuántas operaciones de negocio hace cada iteración
 * @param minNs          mejor tiempo de una iteración, en nanosegundos
 * @param medianNs       mediana de los tiempos
 * @param p95Ns          percentil 95 de los tiempos
 * @param maxNs          peor tiempo
 * @param totalNs        suma de todos los tiempos
 * @param opsPerSecond   throughput estimado
 */
public record BenchmarkResult(
        String scenario,
        int iterations,
        int opsPerIteration,
        long minNs,
        long medianNs,
        long p95Ns,
        long maxNs,
        long totalNs,
        double opsPerSecond) {

    /** Milisegundos con 3 decimales: legible sin perder precisión. */
    public double minMs() {
        return toMs(minNs);
    }

    public double medianMs() {
        return toMs(medianNs);
    }

    public double p95Ms() {
        return toMs(p95Ns);
    }

    public double maxMs() {
        return toMs(maxNs);
    }

    private static double toMs(long nanos) {
        return Math.round(nanos / 1_000.0) / 1_000.0;
    }
}
