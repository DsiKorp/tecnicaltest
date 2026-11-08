package com.dsklabs.epam.tecnicaltest.q07performance;

import com.dsklabs.epam.tecnicaltest.support.web.ApiResponse;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la pregunta 7 (identificar problemas de rendimiento).
 *
 * <p>La secuencia es deliberada y es la respuesta de la entrevista:
 * <ol>
 *   <li>{@code /q07/measure} — <b>medir</b>: cuánto tarda cada paso.</li>
 *   <li>{@code /q07/sampling} — <b>localizar</b>: profiler de muestreo propio.</li>
 *   <li>{@code /q07/jfr} — <b>localizar de verdad</b>: JFR, el profiler del JDK.</li>
 * </ol>
 */
@RestController
@RequestMapping("/q07")
public class PerformanceController {

    private final MeasurementService measurementService;

    public PerformanceController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @GetMapping("/measure")
    public ApiResponse<Map<String, Object>> measure(@RequestParam(defaultValue = "100") int iterations) {
        return ApiResponse.of("q07-measure",
                "Paso 1: medir. Sin medición no hay optimización, solo suposiciones.",
                measurementService.measurePipeline(iterations));
    }

    @GetMapping("/sampling")
    public ApiResponse<Map<String, Object>> sampling(@RequestParam(defaultValue = "2000") int durationMs) {
        long safeDuration = Math.max(200, Math.min(durationMs, 10_000));

        Thread worker = new Thread(() -> {
            long end = System.currentTimeMillis() + safeDuration;
            while (System.currentTimeMillis() < end) {
                burnCpu();
            }
        }, "q07-worker");
        worker.setDaemon(true);

        SamplingProfiler profiler = new SamplingProfiler();
        worker.start();
        SamplingProfiler.SamplingReport report = profiler.profile(worker, safeDuration);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("muestras", report.muestras());
        data.put("omitidas", report.omitidas());
        data.put("hotspots", report.hotspots());
        data.put("hotspotsNegocio", report.hotspotsNegocio());
        data.put("nota",
                "Profiler de muestreo propio: un hilo observa la pila del hilo de trabajo cada "
                        + "milisegundo. Sesgo conocido: solo muestrea hilos RUNNABLE, no los "
                        + "bloqueados. En producción se usaría JFR o async-profiler.");

        return ApiResponse.of("q07-sampling",
                "Paso 2: localizar con un profiler de muestreo", data);
    }

    @GetMapping("/jfr")
    public ApiResponse<Map<String, Object>> jfr(@RequestParam(defaultValue = "2000") int durationMs) {
        long safeDuration = Math.max(200, Math.min(durationMs, 10_000));

        JfrProfiler profiler = new JfrProfiler();
        JfrProfiler.JfrReport report = profiler.profile(safeDuration, () -> {
            long end = System.currentTimeMillis() + safeDuration;
            while (System.currentTimeMillis() < end) {
                burnCpu();
            }
        });

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("muestras", report.muestras());
        data.put("hotspots", report.hotspots());
        data.put("nota",
                "JFR (Java Flight Recorder) es el profiler que trae el JDK: sin dependencias, "
                        + "con muy poca sobrecarga y con eventos de alto nivel (GC, locks, I/O). "
                        + "En producción: -XX:StartFlightRecording=filename=app.jfr,settings=profile");

        return ApiResponse.of("q07-jfr",
                "Paso 2 (mejor): localizar con JFR, el profiler del propio JDK", data);
    }

    /** Trabajo sintético que consume CPU de verdad, para que el profiler tenga algo que ver. */
    private void burnCpu() {
        double total = 0;
        for (int i = 0; i < 50_000; i++) {
            total += Math.sqrt(i);
        }
        if (total == Double.MIN_VALUE) {
            throw new IllegalStateException("imposible");
        }
    }
}
