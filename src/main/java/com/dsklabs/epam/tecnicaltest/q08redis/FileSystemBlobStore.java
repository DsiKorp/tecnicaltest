package com.dsklabs.epam.tecnicaltest.q08redis;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Almacén en sistema de archivos: <b>la respuesta correcta</b> de la pregunta 8.
 *
 * <p>El artículo dice que la mejor solución para "datos enormes" en Redis es no usar Redis,
 * sino un almacén de documentos. Esta implementación representa esa idea de forma concreta:
 *
 * <ul>
 *   <li>No hay límite de 512 MB por item.</li>
 *   <li>El dato no pasa por el heap: se lee chunk a chunk desde disco.</li>
 *   <li>Escala con el disco, no con la RAM del host (que es el recurso escaso).</li>
 * </ul>
 *
 * <p>En un sistema real, esto sería S3, un objeto store, o una base NoSQL de documentos. Aquí
 * se usa el disco local para no depender de infraestructura.
 */
public class FileSystemBlobStore implements ChunkedBlobStore {

    private static final String CHUNK_PREFIX = "chunk-";
    private static final String META_FILE = "__meta";

    private final Path root;
    private final int chunkSize;

    public FileSystemBlobStore(Path root, int chunkSize) {
        this.root = root;
        this.chunkSize = chunkSize;
    }

    @Override
    public String kind() {
        return "file-system";
    }

    @Override
    public BlobMetadata store(String id, byte[] payload) {
        BlobCodec.EncodedBlob encoded = BlobCodec.encode(payload, chunkSize);
        BlobMetadata meta = new BlobMetadata(id, encoded.originalSize(), encoded.storedSize(),
                encoded.chunks().size(), encoded.compressed(), kind());

        Path directory = directoryFor(id);
        try {
            deleteRecursively(directory);
            Files.createDirectories(directory);
            for (int i = 0; i < encoded.chunks().size(); i++) {
                Files.write(directory.resolve(CHUNK_PREFIX + String.format("%06d", i)), encoded.chunks().get(i));
            }
            Files.writeString(directory.resolve(META_FILE), meta.serialize());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el blob '" + id + "' en disco", e);
        }
        return meta;
    }

    @Override
    public byte[] load(String id) {
        BlobMetadata meta = metadata(id);
        if (meta == null) {
            throw new java.util.NoSuchElementException("No existe el blob '" + id + "' en disco");
        }

        List<byte[]> chunks = new ArrayList<>();
        Path directory = directoryFor(id);
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(p -> p.getFileName().toString().startsWith(CHUNK_PREFIX))
                    .sorted()
                    .forEach(p -> {
                        try {
                            chunks.add(Files.readAllBytes(p));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el blob '" + id + "' de disco", e);
        }
        return BlobCodec.decode(chunks, meta.compressed());
    }

    @Override
    public boolean exists(String id) {
        return Files.exists(directoryFor(id).resolve(META_FILE));
    }

    @Override
    public void delete(String id) {
        deleteRecursively(directoryFor(id));
    }

    @Override
    public BlobMetadata metadata(String id) {
        Path metaFile = directoryFor(id).resolve(META_FILE);
        if (!Files.exists(metaFile)) {
            return null;
        }
        try {
            return BlobMetadata.deserialize(Files.readString(metaFile));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudieron leer los metadatos del blob '" + id + "'", e);
        }
    }

    private Path directoryFor(String id) {
        return root.resolve(sanitize(id));
    }

    /** Evita path traversal: un id no puede salir del directorio raíz. */
    private String sanitize(String id) {
        return id.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void deleteRecursively(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo limpiar el directorio " + directory, e);
        }
    }
}
