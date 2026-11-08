package com.dsklabs.epam.tecnicaltest.q10httpget;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Capa UNIT de la pregunta 10. Sin Spring: el servicio se construye con los valores ya
 * resueltos (2048 de presupuesto, 16384 bytes de límite del contenedor).
 */
class GetLimitsServiceTest {

    private final GetLimitsService service = new GetLimitsService(2048, 16384);

    @Test
    @DisplayName("Una URL de 1000 caracteres cabe en el presupuesto práctico de 2048")
    void thousandCharsIsWithinBudget() {
        Map<String, Object> data = service.measure(1000);

        assertThat(data.get("dentroDelPresupuestoPractico")).isEqualTo(true);
        assertThat(data.get("excedeElLimiteDelContenedor")).isEqualTo(false);
        assertThat(data.get("presupuestoPracticoRecomendado")).isEqualTo(2048);
        assertThat(data.get("limiteRealDelContenedorBytes")).isEqualTo(16384);
    }

    @Test
    @DisplayName("Una URL de 3000 caracteres NO cabe en el presupuesto de 2048")
    void threeThousandCharsIsOutOfBudget() {
        Map<String, Object> data = service.measure(3000);

        assertThat(data.get("dentroDelPresupuestoPractico")).isEqualTo(false);
        // 3000 < 16384: el contenedor la aceptaría, pero es una mala idea de diseño.
        assertThat(data.get("excedeElLimiteDelContenedor")).isEqualTo(false);
    }

    @Test
    @DisplayName("La longitud de la URL reportada es exactamente la solicitada")
    void reportedUrlLengthMatchesRequest() {
        assertThat(service.measure(1000).get("longitudDeLaUrl")).isEqualTo(1000);
        assertThat(service.measure(3000).get("longitudDeLaUrl")).isEqualTo(3000);
        assertThat(service.measure(250).get("longitudDeLaUrl")).isEqualTo(250);
        assertThat(service.measure(1000).get("longitudSolicitada")).isEqualTo(1000);
    }

    @Test
    @DisplayName("Por encima del límite del contenedor la URL se marca como excedida")
    void exceedsContainerLimit() {
        Map<String, Object> data = service.measure(20000);

        assertThat(data.get("excedeElLimiteDelContenedor")).isEqualTo(true);
        assertThat(data.get("dentroDelPresupuestoPractico")).isEqualTo(false);
    }

    @Test
    @DisplayName("El contador del anti-patrón (GET que muta) se incrementa en cada llamada")
    void unsafeCounterIncrements() {
        assertThat(service.unsafeIncrement().get("contador")).isEqualTo(1L);
        assertThat(service.unsafeIncrement().get("contador")).isEqualTo(2L);
        assertThat(service.unsafeIncrement().get("contador")).isEqualTo(3L);
    }

    @Test
    @DisplayName("El contador de la variante correcta (POST) también se incrementa")
    void safeCounterIncrements() {
        assertThat(service.safeIncrement().get("contador")).isEqualTo(1L);
        assertThat(service.safeIncrement().get("contador")).isEqualTo(2L);
    }

    @Test
    @DisplayName("Con clave de idempotencia un reintento NO vuelve a incrementar")
    void idempotencyKeyPreventsDoubleIncrement() {
        assertThat(service.safeIncrement("clave-1").get("contador")).isEqualTo(1L);
        assertThat(service.safeIncrement("clave-1").get("contador")).isEqualTo(1L);
        assertThat(service.safeIncrement("clave-2").get("contador")).isEqualTo(2L);
    }

    @Test
    @DisplayName("bodyProbe detecta si el GET ha llegado con cuerpo")
    void bodyProbeDetectsBody() {
        assertThat(service.bodyProbe(null).get("cuerpoRecibido")).isEqualTo(false);
        assertThat(service.bodyProbe("").get("cuerpoRecibido")).isEqualTo(false);
        assertThat(service.bodyProbe("{\"a\":1}").get("cuerpoRecibido")).isEqualTo(true);
        assertThat(service.bodyProbe("hola").get("longitudDelCuerpo")).isEqualTo(4);
    }

    @Test
    @DisplayName("parseSize entiende \"16KB\", \"1MB\", \"512B\" y números sueltos")
    void parseSizeUnderstandsUnits() {
        assertThat(GetLimitsService.parseSize("16KB")).isEqualTo(16384);
        assertThat(GetLimitsService.parseSize("16kb")).isEqualTo(16384);
        assertThat(GetLimitsService.parseSize("1MB")).isEqualTo(1024 * 1024);
        assertThat(GetLimitsService.parseSize("512B")).isEqualTo(512);
        assertThat(GetLimitsService.parseSize("1024")).isEqualTo(1024);
    }
}
