package com.dsklabs.epam.tecnicaltest.q08redis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Codificación de un payload grande: compresión + troceado.
 *
 * <p><b>Es la lógica pura de la pregunta 8</b>, y por eso vive separada de Redis: se puede
 * testear sin Redis (tests unitarios) y la comparten las tres implementaciones de almacén.
 *
 * <p>Las dos técnicas que menciona el artículo:
 * <ul>
 *   <li><b>Comprimir</b> — gzip reduce el payload antes de guardarlo. Ojo: gzip solo
 *       comprime bien si hay redundancia (texto, JSON, CSV). Para datos ya comprimidos o
 *       aleatorios, lo único que hace es añadir CPU y cabecera.</li>
 *   <li><b>Trocear</b> — partir el payload en chunks permite que ninguna operación toque un
 *       item mayor que el límite de Redis (512 MB por item) y evita materializar el blob
 *       entero en el heap al leerlo.</li>
 * </ul>
 */
public final class BlobCodec {

    /** Tamaño por defecto de un chunk: 256 KiB. */
    public static final int DEFAULT_CHUNK_SIZE = 256 * 1024;

    private BlobCodec() {
    }

    /**
     * Comprime (gzip) y trocea el payload.
     *
     * @param payload   datos originales
     * @param chunkSize tamaño máximo de cada chunk
     * @return los chunks más los tamaños
     */
    public static EncodedBlob encode(byte[] payload, int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("El tamaño de chunk debe ser positivo: " + chunkSize);
        }

        byte[] compressed = gzip(payload);
        // Si comprimir ha hecho el payload MÁS grande (datos incompresibles), nos quedamos
        // con el original: comprimir de más es un peaje sin beneficio.
        boolean compressionHelped = compressed.length < payload.length;
        byte[] effective = compressionHelped ? compressed : payload;

        List<byte[]> chunks = new ArrayList<>(Math.max(1, effective.length / chunkSize + 1));
        for (int offset = 0; offset < effective.length; offset += chunkSize) {
            int end = Math.min(offset + chunkSize, effective.length);
            byte[] chunk = new byte[end - offset];
            System.arraycopy(effective, offset, chunk, 0, chunk.length);
            chunks.add(chunk);
        }

        return new EncodedBlob(chunks, payload.length, effective.length, compressionHelped);
    }

    /** Reconstruye el payload original a partir de los chunks. */
    public static byte[] decode(List<byte[]> chunks, boolean compressed) {
        int total = 0;
        for (byte[] chunk : chunks) {
            total += chunk.length;
        }
        byte[] joined = new byte[total];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, joined, offset, chunk.length);
            offset += chunk.length;
        }
        return compressed ? gunzip(joined) : joined;
    }

    private static byte[] gzip(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, data.length / 2));
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(data);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo comprimir el payload", e);
        }
        return out.toByteArray();
    }

    private static byte[] gunzip(byte[] data) {
        try (GZIPInputStream in = new GZIPInputStream(new java.io.ByteArrayInputStream(data))) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo descomprimir el payload", e);
        }
    }

    /**
     * @param chunks        chunks del payload (ya comprimidos si procede)
     * @param originalSize  tamaño del payload antes de comprimir
     * @param storedSize    tamaño realmente almacenado
     * @param compressed    si los chunks van comprimidos con gzip
     */
    public record EncodedBlob(List<byte[]> chunks, long originalSize, long storedSize, boolean compressed) {
    }
}
