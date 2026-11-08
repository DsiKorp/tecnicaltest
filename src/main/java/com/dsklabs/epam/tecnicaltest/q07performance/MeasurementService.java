package com.dsklabs.epam.tecnicaltest.q07performance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * La regla número uno de la pregunta 7: <b>primero medir, después optimizar</b>.
 *
 * <p>Sin medición, una "optimización" es una suposición. Este servicio cronometra cada paso de
 * un pipeline sintético con {@code System.nanoTime()}, que es la técnica de "insertar logs
 * midiendo el tiempo" que menciona el artículo.
 *
 * <p>La alternativa profesional es un profiler (ver {@link SamplingProfiler} y
 * {@link JfrProfiler}), que automatiza el "encontrar la pieza exacta" que aquí se hace a mano.
 */
@Service
public class MeasurementService {

    /**
     * Cronometra cada paso del pipeline.
     *
     * @param iterations cuántas veces se repite el pipeline
     * @return tiempos por paso y total
     */
    public Map<String, Object> measurePipeline(int iterations) {
        int safeIterations = Math.max(1, Math.min(iterations, 10_000));

        List<Map<String, Object>> pasos = new ArrayList<>();
        long totalNanos = 0;

        for (Map.Entry<String, Runnable> paso : pipeline().entrySet()) {
            // Warmup: la primera invocación paga el coste de compilación de la JIT.
            paso.getValue().run();

            long start = System.nanoTime();
            for (int i = 0; i < safeIterations; i++) {
                paso.getValue().run();
            }
            long elapsed = System.nanoTime() - start;
            totalNanos += elapsed;

            Map<String, Object> detalle = new LinkedHashMap<>();
            detalle.put("paso", paso.getKey());
            detalle.put("iteraciones", safeIterations);
            detalle.put("tiempoTotalMs", Math.round(elapsed / 1_000_000.0 * 1000.0) / 1000.0);
            detalle.put("tiempoMedioPorIteracionUs", Math.round(elapsed / (double) safeIterations));
            pasos.add(detalle);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pasos", pasos);
        data.put("tiempoTotalMs", Math.round(totalNanos / 1_000_000.0 * 1000.0) / 1000.0);
        data.put("metodo", "System.nanoTime() con warmup previo (el warmup es clave: sin él se "
                + "mide el coste de compilación JIT, no el del algoritmo)");
        data.put("siguientePaso",
                "Ahora que sabemos QUÉ paso es lento, toca localizar DÓNDentro: "
                        + "un profiler de muestreo o JFR.");

        return data;
    }

    /**
     * Pipeline sintético con un paso deliberadamente caro, para que la medición tenga algo que
     * enseñar.
     */
    private Map<String, Runnable> pipeline() {
        Map<String, Runnable> pipeline = new LinkedHashMap<>();
        pipeline.put("1. Parsear entrada", () -> {
            String payload = "pedido-1234-cliente-ana";
            int sum = 0;
            for (int i = 0; i < payload.length(); i++) {
                sum += payload.charAt(i);
            }
            if (sum == Integer.MIN_VALUE) {
                throw new IllegalStateException("imposible");
            }
        });
        pipeline.put("2. Validar reglas de negocio", () -> {
            boolean valid = "pedido-1234-cliente-ana".length() > 3;
            if (!valid) {
                throw new IllegalStateException("imposible");
            }
        });
        pipeline.put("3. Calcular total (el paso caro)", () -> {
            long total = 0;
            for (int i = 0; i < 200_000; i++) {
                total += Math.sqrt(i) * 1.0001;
            }
            if (total == Long.MIN_VALUE) {
                throw new IllegalStateException("imposible");
            }
        });
        pipeline.put("4. Serializar respuesta", () -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 500; i++) {
                sb.append("item-").append(i).append(';');
            }
            if (sb.length() == Integer.MIN_VALUE) {
                throw new IllegalStateException("imposible");
            }
        });
        return pipeline;
    }
}
