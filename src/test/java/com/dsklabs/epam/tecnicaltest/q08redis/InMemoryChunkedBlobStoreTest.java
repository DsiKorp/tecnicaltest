package com.dsklabs.epam.tecnicaltest.q08redis;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Capa UNIT de la pregunta 8: el almacén en memoria (el fallback sin Redis). */
class InMemoryChunkedBlobStoreTest {

    @Test
    @DisplayName("Guardar y recuperar devuelve el payload idéntico")
    void storeAndLoad() {
        InMemoryChunkedBlobStore store = new InMemoryChunkedBlobStore(256);
        byte[] payload = "contenido de prueba".repeat(50).getBytes(StandardCharsets.UTF_8);

        store.store("id-1", payload);

        assertThat(store.load("id-1")).isEqualTo(payload);
        assertThat(store.exists("id-1")).isTrue();
    }

    @Test
    @DisplayName("Los metadatos describen lo guardado")
    void metadata() {
        InMemoryChunkedBlobStore store = new InMemoryChunkedBlobStore(64);
        // Datos incompresibles: con ceros, gzip los encoge y quedaría en un solo chunk.
        byte[] payload = new byte[1000];
        new java.util.Random(7).nextBytes(payload);

        BlobMetadata meta = store.store("id-2", payload);

        assertThat(meta.id()).isEqualTo("id-2");
        assertThat(meta.originalSize()).isEqualTo(1000);
        assertThat(meta.chunkCount()).isGreaterThan(1);
        assertThat(meta.storeKind()).isEqualTo("in-memory");
        assertThat(store.metadata("id-2")).isEqualTo(meta);
    }

    @Test
    @DisplayName("Guardar el mismo id otra vez sobrescribe")
    void overwrite() {
        InMemoryChunkedBlobStore store = new InMemoryChunkedBlobStore(256);

        store.store("id-3", "primero".getBytes(StandardCharsets.UTF_8));
        store.store("id-3", "segundo".getBytes(StandardCharsets.UTF_8));

        assertThat(store.load("id-3")).isEqualTo("segundo".getBytes(StandardCharsets.UTF_8));
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Cargar un id inexistente lanza NoSuchElementException")
    void loadMissing() {
        InMemoryChunkedBlobStore store = new InMemoryChunkedBlobStore(256);

        assertThatThrownBy(() -> store.load("no-existe"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("Eliminar quita el blob")
    void delete() {
        InMemoryChunkedBlobStore store = new InMemoryChunkedBlobStore(256);
        store.store("id-4", "x".getBytes(StandardCharsets.UTF_8));

        store.delete("id-4");

        assertThat(store.exists("id-4")).isFalse();
        assertThat(store.metadata("id-4")).isNull();
    }
}
