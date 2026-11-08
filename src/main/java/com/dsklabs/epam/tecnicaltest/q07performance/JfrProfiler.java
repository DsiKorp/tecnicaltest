package com.dsklabs.epam.tecnicaltest.q07performance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordingFile;

/**
 * Profiler real usando <b>JFR</b> (Java Flight Recorder), el profiler que trae el propio JDK.
 *
 * <p>Por qué esto es la mejor respuesta de la pregunta 7: no hace falta instalar nada. En
 * producción se arranca con {@code -XX:StartFlightRecording=filename=app.jfr,settings=profile}
 * y se analiza con la JFR Console o con {@code jfr print}. En esta demo se graba en memoria y
 * se leen los eventos con {@link RecordingFile}, sin dependencias extra.
 *
 * <p>Se usa el evento {@code jdk.ExecutionSample}: el runtime toma muestras periódicas de la
 * pila de los hilos en ejecución. Agregando la hoja de cada muestra se obtiene el mismo
 * histograma que da un profiler de muestreo, pero con la instrumentación del propio JDK.
 */
public class JfrProfiler {

    /**
     * Graba una ejecución y devuelve los métodos más calientes.
     *
     * @param durationMs duración de la grabación
     * @param work        trabajo a ejecutar mientras se graba
     * @return los métodos más muestreados
     */
    public JfrReport profile(long durationMs, Runnable work) {
        Path recordingFile;
        try (Recording recording = new Recording()) {
            recording.enable("jdk.ExecutionSample").withPeriod(java.time.Duration.ofMillis(10));
            recording.enable("jdk.NativeMethodSample").withPeriod(java.time.Duration.ofMillis(20));
            recording.start();

            work.run();

            recording.stop();
            recordingFile = Files.createTempFile("q07-jfr-", ".jfr");
            recording.dump(recordingFile);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir la grabación JFR", e);
        }

        final Path archivo = recordingFile;
        return read(archivo);
    }

    /** Lee la grabación y agrega las hojas de las muestras de ejecución. */
    private JfrReport read(Path recordingFile) {
        Map<String, Long> histogram = new LinkedHashMap<>();
        long totalSamples = 0;

        try (RecordingFile file = new RecordingFile(recordingFile)) {
            while (file.hasMoreEvents()) {
                RecordedEvent event = file.readEvent();
                if (!"jdk.ExecutionSample".equals(event.getEventType().getName())) {
                    continue;
                }
                RecordedFrame top = event.getStackTrace().getFrames().isEmpty()
                        ? null
                        : event.getStackTrace().getFrames().get(0);
                if (top == null) {
                    continue;
                }
                String key = top.getMethod().getType().getName() + "." + top.getMethod().getName();
                histogram.merge(key, 1L, Long::sum);
                totalSamples++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la grabación JFR", e);
        } finally {
            try {
                Files.deleteIfExists(recordingFile);
            } catch (IOException ignored) {
                // El archivo temporal no es crítico.
            }
        }

        // `totalSamples` se incrementa en el bucle, así que no es effectively final y no se
        // puede capturar en la lambda: se copia a una final.
        final long muestras = totalSamples;

        Map<String, Double> hotspots = histogram.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Math.round(entry.getValue() * 1000.0 / Math.max(1, muestras)) / 10.0,
                        (a, b) -> a,
                        LinkedHashMap::new));

        return new JfrReport(totalSamples, hotspots, recordingFile.getFileName().toString());
    }

    /**
     * @param muestras  número de muestras de ejecución capturadas
     * @param hotspots  métodos más calientes con su porcentaje
     * @param archivo   nombre del archivo de grabación temporal
     */
    public record JfrReport(long muestras, Map<String, Double> hotspots, String archivo) {
    }
}
