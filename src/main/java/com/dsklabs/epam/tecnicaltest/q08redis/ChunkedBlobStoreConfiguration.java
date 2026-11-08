package com.dsklabs.epam.tecnicaltest.q08redis;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Decide qué implementación de {@link ChunkedBlobStore} se usa, <b>sin romper el arranque</b>.
 *
 * <p>Modos ({@code epam.q08.mode}):
 * <ul>
 *   <li>{@code auto} (por defecto) — hace <b>un</b> PING a Redis con timeout corto. Si responde,
 *       usa Redis; si no, cae al almacén en memoria y deja un WARN en el log.</li>
 *   <li>{@code redis} — obliga Redis (falla rápido si no está).</li>
 *   <li>{@code memory} — nunca toca Redis.</li>
 * </ul>
 *
 * <p><b>Por qué el fallback importa:</b> en la instancia EC2 de AWS no hay Redis. Si la
 * aplicación dependiera de Redis para arrancar, el despliegue se caería. Con este diseño, la
 * <b>misma imagen</b> sirve en local (con Redis vía docker-compose) y en AWS (sin Redis), y el
 * endpoint {@code /q08/store} informa de qué implementación está viva.
 *
 * <p>Además, la conexión de Lettuce es perezosa: que no haya Redis nunca impide el arranque.
 */
@Configuration
public class ChunkedBlobStoreConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ChunkedBlobStoreConfiguration.class);

    private final String mode;
    private final int chunkSize;
    private final String fileRoot;
    private final ObjectProvider<RedisConnectionFactory> connectionFactoryProvider;
    private final ObjectProvider<StringRedisTemplate> templateProvider;

    private volatile String activeKind = "in-memory";
    private volatile String reason = "Redis no configurado";

    public ChunkedBlobStoreConfiguration(
            @Value("${epam.q08.mode:auto}") String mode,
            @Value("${epam.q08.chunk-size:262144}") int chunkSize,
            @Value("${epam.q08.file-root:${java.io.tmpdir}/epam-q08-blobs}") String fileRoot,
            ObjectProvider<RedisConnectionFactory> connectionFactoryProvider,
            ObjectProvider<StringRedisTemplate> templateProvider) {
        this.mode = mode == null ? "auto" : mode.toLowerCase();
        this.chunkSize = chunkSize;
        this.fileRoot = fileRoot;
        this.connectionFactoryProvider = connectionFactoryProvider;
        this.templateProvider = templateProvider;
    }

    @Bean
    @Primary
    public ChunkedBlobStore chunkedBlobStore() {
        return switch (mode) {
            case "memory" -> {
                activeKind = "in-memory";
                reason = "modo 'memory' forzado por configuración";
                log.info("P8: almacén en memoria (modo forzado 'memory')");
                yield new InMemoryChunkedBlobStore(chunkSize);
            }
            case "file" -> {
                activeKind = "file-system";
                reason = "modo 'file' forzado: almacén en disco (la respuesta correcta)";
                log.info("P8: almacén en disco (modo forzado 'file')");
                yield new FileSystemBlobStore(Path.of(fileRoot), chunkSize);
            }
            case "redis" -> {
                StringRedisTemplate template = requireTemplate();
                checkRedis(template);
                activeKind = "redis";
                reason = "Redis disponible";
                log.info("P8: usando Redis como almacén de blobs");
                yield new RedisChunkedBlobStore(template, chunkSize);
            }
            default -> {
                StringRedisTemplate template = templateProvider.getIfAvailable();
                if (template != null && pingSucceeds(template)) {
                    activeKind = "redis";
                    reason = "Redis disponible (detectado automáticamente)";
                    log.info("P8: Redis detectado, usando Redis como almacén de blobs");
                    yield new RedisChunkedBlobStore(template, chunkSize);
                }
                activeKind = "in-memory";
                reason = "Redis no responde: fallback al almacén en memoria";
                log.warn("P8: Redis no está disponible, se usa el almacén EN MEMORIA "
                        + "(en AWS es el comportamiento esperado: no hay Redis en la instancia)");
                yield new InMemoryChunkedBlobStore(chunkSize);
            }
        };
    }

    /**
     * Almacén en disco: la respuesta correcta de la pregunta ("no uses Redis para esto").
     * No es el bean por defecto, pero se puede pedir con {@code epam.q08.mode=file}.
     */
    @Bean
    public FileSystemBlobStore fileSystemBlobStore() {
        return new FileSystemBlobStore(Path.of(fileRoot), chunkSize);
    }

    private StringRedisTemplate requireTemplate() {
        StringRedisTemplate template = templateProvider.getIfAvailable();
        if (template == null) {
            throw new IllegalStateException(
                    "epam.q08.mode=redis pero no hay cliente de Redis en el classpath");
        }
        return template;
    }

    private void checkRedis(StringRedisTemplate template) {
        if (!pingSucceeds(template)) {
            throw new IllegalStateException(
                    "epam.q08.mode=redis pero Redis no responde. "
                            + "Usa EPAM_Q08_MODE=auto para caer al almacén en memoria.");
        }
    }

    /** Un único PING con timeout corto. Si falla, no es un error: es el fallback. */
    private boolean pingSucceeds(StringRedisTemplate template) {
        try {
            template.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.debug("Ping a Redis falló: {}", e.getMessage());
            return false;
        }
    }

    /** Tipo de almacén activo ("redis", "in-memory"). */
    public String activeKind() {
        return activeKind;
    }

    /** Motivo por el que se eligió ese almacén. */
    public String reason() {
        return reason;
    }

    /** Tamaño de chunk configurado. */
    public int chunkSize() {
        return chunkSize;
    }

    /** Modo configurado. */
    public String mode() {
        return mode;
    }
}
