package com.dsklabs.epam.tecnicaltest.q06exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Capa UNIT de la pregunta 6: la fault barrier y la jerarquía de excepciones.
 *
 * <p>Sin Spring: la lógica de negocio se prueba con un repositorio en memoria.
 */
class OrderServiceTest {

    private InMemoryOrderRepository repository;
    private OrderService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        service = new OrderService(repository);
    }

    @Test
    @DisplayName("Un pedido válido se confirma")
    void happyPath() throws Exception {
        String resultado = service.placeOrder("ORD-1", "SKU-1", 2);

        assertThat(resultado).contains("ORD-1").contains("confirmado");
    }

    @Test
    @DisplayName("Sin stock suficiente se lanza la excepción de negocio concreta")
    void insufficientStock() {
        // SKU-2 solo tiene 1 unidad
        assertThatThrownBy(() -> service.placeOrder("ORD-2", "SKU-2", 5))
                .isInstanceOf(InsufficientStockException.class)
                .isInstanceOf(BusinessException.class)
                .isInstanceOf(AppException.class)
                .hasMessageContaining("SKU-2");
    }

    @Test
    @DisplayName("La fault barrier convierte un fallo técnico en un fallo de negocio")
    void faultBarrierTranslatesTechnicalFailures() {
        repository.setFailing(true);

        assertThatThrownBy(() -> service.placeOrder("ORD-3", "SKU-1", 1))
                .isInstanceOf(OrderRejectedException.class)
                .isInstanceOf(BusinessException.class)
                .satisfies(thrown -> {
                    // El detalle técnico NO está en el mensaje que ve el cliente...
                    assertThat(thrown.getMessage()).doesNotContain("SQLSTATE");
                    assertThat(thrown.getMessage()).contains("Inténtalo más tarde");
                    // ...pero se conserva en la causa para poder depurar.
                    assertThat(thrown.getCause())
                            .isInstanceOf(PersistenceException.class)
                            .hasMessageContaining("SQLSTATE");
                });
    }

    @Test
    @DisplayName("La excepción de negocio lleva un código de error estable")
    void errorCodeIsStable() {
        assertThatThrownBy(() -> service.placeOrder("ORD-4", "SKU-2", 5))
                .isInstanceOf(AppException.class)
                .satisfies(thrown -> assertThat(((AppException) thrown).errorCode())
                        .isEqualTo("INSUFFICIENT_STOCK"));
    }

    @Test
    @DisplayName("tryPlaceOrder devuelve un Result en vez de lanzar excepción")
    void tryPlaceOrderReturnsResult() {
        Result<String> resultado = service.tryPlaceOrder("ORD-5", "SKU-1", 1);

        assertThat(resultado.isSuccess()).isTrue();
        assertThat(resultado.valueOrThrow()).contains("ORD-5");
    }

    @Test
    @DisplayName("tryPlaceOrder devuelve un Failure cuando el fallo es esperado")
    void tryPlaceOrderReturnsFailure() {
        Result<String> resultado = service.tryPlaceOrder("ORD-6", "SKU-2", 99);

        assertThat(resultado.isSuccess()).isFalse();
        assertThat(resultado).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?>) resultado).errorCode()).isEqualTo("INSUFFICIENT_STOCK");
    }

    @Test
    @DisplayName("Una cantidad no positiva se rechaza como fallo de negocio")
    void nonPositiveQuantity() {
        assertThatThrownBy(() -> service.placeOrder("ORD-7", "SKU-1", 0))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("Un SKU inexistente se trata como stock 0")
    void unknownSku() {
        assertThatThrownBy(() -> service.placeOrder("ORD-8", "SKU-NO-EXISTE", 1))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("disponible 0");
    }
}
