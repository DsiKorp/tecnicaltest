package com.dsklabs.epam.tecnicaltest.q08redis;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Almacén sobre Redis: un HASH por blob.
 *
 * <p>Estructura en Redis:
 * <pre>
 *   HSET epam:q08:blob:{id} __meta "id|originalSize|storedSize|chunkCount|compressed|kind"
 *   HSET epam:q08:blob:{id} chunk:000000 "&lt;base64 del chunk&gt;"
 *   HSET epam:q08:blob:{id} chunk:000001 "..."
 * </pre>
 *
 * <p>La lectura se hace con <b>{@code HGETALL}</b>: el comando que nombra el artículo. Trae el
 * hash entero de una vez, que es lo correcto para un blob que se lee completo.
 *
 * <p><b>Las limitaciones que hay que saber:</b>
 * <ul>
 *   <li>Redis tiene un límite de <b>512 MB por item</b>: un solo HASH no puede pasar de ahí.
 *       Trocear en varios items (y no en campos) es la alternativa cuando el blob es enorme.</li>
 *   <li>Mover mucho dato por la red y guardarlo en RAM es caro. Redis es una <b>caché</b>, no
 *       un almacén de datos.</li>
 *   <li>El patrón "leer el valor enorme entero" está entre los peores prácticas de Redis
 *       (redis.com/blog/7-redis-worst-practices/).</li>
 * </ul>
 */
public class RedisChunkedBlobStore implements ChunkedBlobStore {

    private static final String KEY_PREFIX = "epam:q08:blob:";
    private static final String META_FIELD = "__meta";
    private static final String CHUNK_PREFIX = "chunk:";

    private final StringRedisTemplate redis;
    private final int chunkSize;

    public RedisChunkedBlobStore(StringRedisTemplate redis, int chunkSize) {
        this.redis = redis;
        this.chunkSize = chunkSize;
    }

    @Override
    public String kind() {
        return "redis";
    }

    @Override
    public BlobMetadata store(String id, byte[] payload) {
        BlobCodec.EncodedBlob encoded = BlobCodec.encode(payload, chunkSize);
        BlobMetadata meta = new BlobMetadata(id, encoded.originalSize(), encoded.storedSize(),
                encoded.chunks().size(), encoded.compressed(), kind());

        String key = KEY_PREFIX + id;
        // Primero borramos por si el id ya existía (sobrescribir de forma limpia).
        redis.delete(key);

        List<String> fields = new ArrayList<>();
        List<String> values = new ArrayList<>();
        fields.add(META_FIELD);
        values.add(meta.serialize());

        Base64.Encoder encoder = Base64.getEncoder();
        for (int i = 0; i < encoded.chunks().size(); i++) {
            fields.add(CHUNK_PREFIX + String.format("%06d", i));
            values.add(encoder.encodeToString(encoded.chunks().get(i)));
        }

        redis.<Object, Object>opsForHash().putAll(key, toMap(fields, values));
        return meta;
    }

    @Override
    public byte[] load(String id) {
        Map<Object, Object> raw = redis.opsForHash().entries(KEY_PREFIX + id);
        if (raw == null || raw.isEmpty()) {
            throw new java.util.NoSuchElementException("No existe el blob '" + id + "' en Redis");
        }

        BlobMetadata meta = BlobMetadata.deserialize((String) raw.get(META_FIELD));

        List<byte[]> chunks = new ArrayList<>();
        Base64.Decoder decoder = Base64.getDecoder();
        raw.entrySet().stream()
                .filter(entry -> String.valueOf(entry.getKey()).startsWith(CHUNK_PREFIX))
                // Las claves del HASH son Object (no Comparable): se ordenan por su forma
                // textual, que es el nombre del campo ("chunk:000000", "chunk:000001"...).
                .sorted(java.util.Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                .forEach(entry -> chunks.add(decoder.decode(String.valueOf(entry.getValue()))));

        return BlobCodec.decode(chunks, meta.compressed());
    }

    @Override
    public boolean exists(String id) {
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + id));
    }

    @Override
    public void delete(String id) {
        redis.delete(KEY_PREFIX + id);
    }

    @Override
    public BlobMetadata metadata(String id) {
        Object meta = redis.opsForHash().get(KEY_PREFIX + id, META_FIELD);
        return meta == null ? null : BlobMetadata.deserialize((String) meta);
    }

    private Map<Object, Object> toMap(List<String> fields, List<String> values) {
        Map<Object, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            map.put(fields.get(i), values.get(i));
        }
        return map;
    }

    /** Clave que usa un blob en Redis (para la demo). */
    public static String keyFor(String id) {
        return KEY_PREFIX + id;
    }

    /** Codificación de los metadatos en el HASH (visible en redis-cli). */
    public String metaFieldName() {
        return META_FIELD;
    }

    /** Prefijo de los campos de chunk (visible en redis-cli). */
    public String chunkFieldPrefix() {
        return CHUNK_PREFIX;
    }

    /** Codificación por defecto (UTF-8) usada para los metadatos. */
    public static java.nio.charset.Charset charset() {
        return StandardCharsets.UTF_8;
    }
}
