package com.dsklabs.epam.tecnicaltest.q07performance;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Profiler de muestreo propio: un hilo que toma la traza de la pila de otro hilo cada
 * milisegundos y agrega las hojas en un histograma.
 *
 * <p>Es la técnica "clásica" de profiling por muestreo (el mismo criterio que usan
 * VisualVM, YourKit o async-profiler): no se instrumenta el código, se observa de fuera. El
 * sesgo es conocido — solo se muestrea en los hilos que están en estado RUNNABLE, no en los
 * bloqueados — pero para localizar el <b>método que se lleva el tiempo de CPU</b> es más que
 * suficiente y no requiere ninguna dependencia.
 *
 * <p>Alternativa profesional: <b>JFR</b> (ver {@link JfrProfiler}), que es el profiler que trae
 * el propio JDK y el que se usaría en producción.
 */
public class SamplingProfiler implements AutoCloseable {

    /** Intervalo entre muestras, en nanosegundos (1 ms). */
    private static final long SAMPLE_INTERVAL_NANOS = 1_000_000L;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong samples = new AtomicLong();
    private final AtomicInteger skipped = new AtomicInteger();

    /** Hoja de la traza -> número de veces que se ha visto. */
    private final Map<String, AtomicLong> histogram = new ConcurrentHashMap<>();

    /** Método completo (clase.metodo) -> número de veces. */
    private final Map<String, AtomicLong> methodHistogram = new ConcurrentHashMap<>();

    private volatile Thread target;
    private volatile Thread sampler;

    /**
     * Muestrea el hilo indicado durante el tiempo pedido.
     *
     * @param thread      hilo a observar (normalmente el que hace el trabajo)
     * @param durationMs  duración del muestreo
     * @return el resultado del muestreo
     */
    public SamplingReport profile(Thread thread, long durationMs) {
        this.target = thread;
        running.set(true);
        samples.set(0);
        skipped.set(0);
        histogram.clear();
        methodHistogram.clear();

        sampler = new Thread(this::sampleLoop, "sampling-profiler");
        sampler.setDaemon(true);
        sampler.start();

        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            running.set(false);
            if (sampler != null) {
                try {
                    sampler.join(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return report();
    }

    private void sampleLoop() {
        while (running.get()) {
            Thread observed = target;
            if (observed != null && observed.getState() == Thread.State.RUNNABLE) {
                StackTraceElement[] stack = observed.getStackTrace();
                if (stack.length > 0) {
                    // La hoja es el método donde se está ejecutando el hilo AHORA.
                    StackTraceElement leaf = stack[0];
                    histogram.computeIfAbsent(leaf.getClassName() + "." + leaf.getMethodName(),
                            k -> new AtomicLong()).incrementAndGet();
                    samples.incrementAndGet();

                    // Y el método "de usuario" más alto en la pila: da contexto de negocio.
                    for (StackTraceElement element : stack) {
                        String className = element.getClassName();
                        if (className.startsWith("com.dsklabs.epam.tecnicaltest")) {
                            methodHistogram.computeIfAbsent(className + "." + element.getMethodName(),
                                    k -> new AtomicLong()).incrementAndGet();
                            break;
                        }
                    }
                }
            } else {
                skipped.incrementAndGet();
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private SamplingReport report() {
        List<Map.Entry<String, Long>> top = histogram.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().get()))
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15)
                .toList();

        List<Map.Entry<String, Long>> topBusiness = methodHistogram.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().get()))
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .toList();

        long total = Math.max(1, samples.get());
        Map<String, Double> percentages = new LinkedHashMap<>();
        top.forEach(entry -> percentages.put(entry.getKey(),
                Math.round(entry.getValue() * 1000.0 / total) / 10.0));

        return new SamplingReport(total, skipped.get(), percentages, topBusiness);
    }

    @Override
    public void close() {
        running.set(false);
    }

    /**
     * @param muestras        número de muestras tomadas
     * @param omitidas        muestreos en los que el hilo no estaba RUNNABLE
     * @param hotspots        métodos hoja con su porcentaje
     * @param hotspotsNegocio métodos del proyecto con su porcentaje
     */
    public record SamplingReport(long muestras, int omitidas, Map<String, Double> hotspots,
                                  List<Map.Entry<String, Long>> hotspotsNegocio) {
    }
}
