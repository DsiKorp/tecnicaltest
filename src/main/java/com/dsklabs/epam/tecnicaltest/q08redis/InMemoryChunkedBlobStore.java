package com.dsklabs.epam.tecnicaltest.q08redis;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén en memoria. Es el <b>fallback</b> que se usa cuando no hay Redis.
 *
 * <p>Es el que hace que la app arranque igual en local (con Redis en docker-compose) y en AWS
 * (sin Redis): la pregunta 8 nunca rompe el arranque de la aplicación.
 */
public class InMemoryChunkedBlobStore implements ChunkedBlobStore {

    private final Map<String, BlobCodec.EncodedBlob> blobs = new ConcurrentHashMap<>();
    private final Map<String, BlobMetadata> metadata = new ConcurrentHashMap<>();
    private final int chunkSize;

    public InMemoryChunkedBlobStore(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Override
    public String kind() {
        return "in-memory";
    }

    @Override
    public BlobMetadata store(String id, byte[] payload) {
        BlobCodec.EncodedBlob encoded = BlobCodec.encode(payload, chunkSize);
        blobs.put(id, encoded);
        BlobMetadata meta = new BlobMetadata(id, encoded.originalSize(), encoded.storedSize(),
                encoded.chunks().size(), encoded.compressed(), kind());
        metadata.put(id, meta);
        return meta;
    }

    @Override
    public byte[] load(String id) {
        BlobCodec.EncodedBlob encoded = blobs.get(id);
        if (encoded == null) {
            throw new java.util.NoSuchElementException("No existe el blob '" + id + "'");
        }
        return BlobCodec.decode(encoded.chunks(), encoded.compressed());
    }

    @Override
    public boolean exists(String id) {
        return blobs.containsKey(id);
    }

    @Override
    public void delete(String id) {
        blobs.remove(id);
        metadata.remove(id);
    }

    @Override
    public BlobMetadata metadata(String id) {
        return metadata.get(id);
    }

    /** Número de blobs guardados (para la demo). */
    public int size() {
        return blobs.size();
    }
}
