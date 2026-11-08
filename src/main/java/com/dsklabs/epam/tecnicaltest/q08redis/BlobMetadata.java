package com.dsklabs.epam.tecnicaltest.q08redis;

/**
 * Metadatos de un blob almacenado.
 *
 * @param id           identificador del blob
 * @param originalSize tamaño del payload antes de comprimir (bytes)
 * @param storedSize   tamaño real almacenado tras comprimir (bytes)
 * @param chunkCount   número de chunks en los que se troceó
 * @param compressed   si el payload se comprimió con gzip
 * @param storeKind    implementación que lo guardó ("redis", "in-memory", "file-system")
 */
public record BlobMetadata(
        String id,
        long originalSize,
        long storedSize,
        int chunkCount,
        boolean compressed,
        String storeKind) {

    /**
     * Serializa a un string plano delimitado por {@code |}.
     *
     * <p>Deliberadamente no usamos JSON: importarlo ataría el modelo a una librería de
     * serialización concreta (y en Boot 4 es Jackson 3, no Jackson 2). Un formato OWN simple
     * es suficiente para metadatos internos y se puede parsear en dos líneas.
     */
    public String serialize() {
        return String.join("|", id, String.valueOf(originalSize), String.valueOf(storedSize),
                String.valueOf(chunkCount), String.valueOf(compressed), storeKind);
    }

    /** Reconstruye los metadatos desde su forma serializada. */
    public static BlobMetadata deserialize(String raw) {
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 6) {
            throw new IllegalArgumentException("Metadatos con formato inesperado: " + raw);
        }
        return new BlobMetadata(
                parts[0],
                Long.parseLong(parts[1]),
                Long.parseLong(parts[2]),
                Integer.parseInt(parts[3]),
                Boolean.parseBoolean(parts[4]),
                parts[5]);
    }

    /** Ratio de compresión (payload original / payload almacenado). */
    public double compressionRatio() {
        return storedSize == 0 ? 1.0 : Math.round((originalSize / (double) storedSize) * 1000.0) / 1000.0;
    }
}
