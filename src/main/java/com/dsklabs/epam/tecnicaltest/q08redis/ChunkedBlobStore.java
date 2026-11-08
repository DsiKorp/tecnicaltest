package com.dsklabs.epam.tecnicaltest.q08redis;

/**
 * Contrato de un almacén de blobs grandes troceados.
 *
 * <p>Tres implementaciones, y la respuesta de la pregunta 8 es justamente que <b>Redis no es
 * la herramienta correcta</b>:
 *
 * <ul>
 *   <li>{@link RedisChunkedBlobStore} — el síntoma: trocear y comprimir para meterlo en
 *       Redis, aceptando el límite de 512 MB por item y el coste de mover mucho dato.</li>
 *   <li>{@link InMemoryChunkedBlobStore} — el fallback para cuando no hay Redis (en la
 *       instancia EC2 de AWS no lo hay).</li>
 *   <li>{@link FileSystemBlobStore} — <b>la respuesta correcta</b>: un almacén de documentos
 *       (o una base NoSQL), donde el dato no pasa por el heap ni tiene un límite de item.</li>
 * </ul>
 */
public interface ChunkedBlobStore {

    /** Identificador de la implementación, para poder reportarla en la demo. */
    String kind();

    /**
     * Guarda un payload (comprimido y troceado).
     *
     * @return los metadatos de lo guardado
     */
    BlobMetadata store(String id, byte[] payload);

    /** Recupera el payload original. */
    byte[] load(String id);

    boolean exists(String id);

    void delete(String id);

    /** Metadatos de un blob almacenado, o null si no existe. */
    BlobMetadata metadata(String id);
}
