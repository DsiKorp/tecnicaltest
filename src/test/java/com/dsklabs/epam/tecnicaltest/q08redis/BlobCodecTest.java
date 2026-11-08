package com.dsklabs.epam.tecnicaltest.q08redis;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Capa UNIT de la pregunta 8: la lógica de compresión y troceado, sin Redis.
 *
 * <p>Que esta lógica viva separada del almacén es justo lo que hace testeable el núcleo del
 * problema: el 90% del riesgo está en "comprimir y trocear bien", no en el comando de Redis.
 */
class BlobCodecTest {

    @Test
    @DisplayName("El round-trip comprime y reconstruye el payload idéntico")
    void roundTripIsLossless() {
        byte[] payload = "hola mundo".repeat(1000).getBytes(StandardCharsets.UTF_8);

        BlobCodec.EncodedBlob encoded = BlobCodec.encode(payload, 64);
        byte[] decoded = BlobCodec.decode(encoded.chunks(), encoded.compressed());

        assertThat(decoded).isEqualTo(payload);
    }

    @Test
    @DisplayName("El payload se trocea en chunks del tamaño pedido")
    void chunksRespectTheSize() {
        // Datos INCOMPRESIBLES: con datos repetitivos gzip los encoge y el número de chunks
        // cambia. Aquí se quiere verificar el troceado, así que el payload debe Resistir
        // la compresión.
        byte[] payload = datosIncompresibles(1000);

        BlobCodec.EncodedBlob encoded = BlobCodec.encode(payload, 256);

        // 1000 bytes / 256 = 4 chunks (3 completos + 1 de 232 bytes)
        assertThat(encoded.chunks()).hasSize(4);
        assertThat(encoded.chunks()).allSatisfy(chunk -> assertThat(chunk.length).isLessThanOrEqualTo(256));
    }

    /** Bytes pseudoaleatorios deterministas: no se comprimen. */
    private byte[] datosIncompresibles(int size) {
        byte[] data = new byte[size];
        new java.util.Random(7).nextBytes(data);
        return data;
    }

    @Test
    @DisplayName("Un payload vacío produce cero chunks y no revienta")
    void emptyPayload() {
        BlobCodec.EncodedBlob encoded = BlobCodec.encode(new byte[0], 256);

        assertThat(encoded.chunks()).isEmpty();
        assertThat(BlobCodec.decode(encoded.chunks(), encoded.compressed())).isEmpty();
    }

    @Test
    @DisplayName("Gzip reduce un payload muy repetitivo")
    void gzipCompressesRepetitiveData() {
        byte[] payload = "A".repeat(100_000).getBytes(StandardCharsets.UTF_8);

        BlobCodec.EncodedBlob encoded = BlobCodec.encode(payload, 4096);

        assertThat(encoded.compressed()).isTrue();
        assertThat(encoded.storedSize()).isLessThan(encoded.originalSize());
        assertThat(encoded.originalSize()).isEqualTo(100_000);
    }

    @Test
    @DisplayName("Con datos incompresibles no se comprime (comprimir de más es un peaje)")
    void doesNotCompressWhenItDoesNotHelp() {
        byte[] payload = new byte[10_000];
        new java.util.Random(42).nextBytes(payload);

        BlobCodec.EncodedBlob encoded = BlobCodec.encode(payload, 4096);

        // gzip sobre datos aleatorios suele ser MAYOR que el original: en ese caso nos quedamos
        // con el original y marcamos compressed=false.
        assertThat(encoded.storedSize()).isLessThanOrEqualTo(encoded.originalSize());
        assertThat(BlobCodec.decode(encoded.chunks(), encoded.compressed())).isEqualTo(payload);
    }

    @Test
    @DisplayName("Un chunk size inválido se rechaza")
    void invalidChunkSize() {
        assertThatThrownBy(() -> BlobCodec.encode(new byte[10], 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Los metadatos se serializan y deserializan sin perder información")
    void metadataRoundTrip() {
        BlobMetadata original = new BlobMetadata("blob-1", 1000, 250, 4, true, "redis");

        BlobMetadata restored = BlobMetadata.deserialize(original.serialize());

        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("El ratio de compresión se calcula sobre el tamaño real")
    void compressionRatio() {
        BlobMetadata meta = new BlobMetadata("blob-1", 1000, 250, 4, true, "redis");

        assertThat(meta.compressionRatio()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("Los metadatos con formato inválido se rechazan")
    void invalidMetadata() {
        assertThatThrownBy(() -> BlobMetadata.deserialize("no|valid|metadata"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
