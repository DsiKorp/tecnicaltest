package com.dsklabs.epam.tecnicaltest.q04testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Capa E2E (UI) de la pirámide de testing.
 *
 * <p>Levanta el servidor real y recorre <b>las diez demos</b> tal y como lo haría una persona
 * en la entrevista: cada endpoint tiene que devolver 200 y un payload con la clave {@code demo}.
 *
 * <p>Es el test que da la garantía final de que "la demo funciona": si algo se rompe en
 * cualquier capa, este test se pone rojo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class DemosEndToEndTest {

    @Autowired
    private RestTestClient client;

    @ParameterizedTest(name = "La demo {0} responde 200 con su payload")
    @CsvSource({
            "/q01/internals?elements=3,              q01-internals",
            "/q01/benchmark?size=1000&iterations=2,   q01-collections",
            "/q02/rest/greeting?name=Ana,            q02-rest",
            "/q02/compare,                            q02-compare",
            "/q03/summary,                            q03-gitops",
            "/q04/pyramid,                            q04-testing",
            "/q05/order?strategy=tree&size=10,        q05-hashset",
            "/q06/hierarchy,                          q06-hierarchy",
            "/q07/measure?iterations=5,               q07-measure",
            "/q08/store,                              q08-store",
            "/q09/overhead?iterations=1000,           q09-aop",
            "/q10/measure?length=3000,               q10-http-get"
    })
    void demoResponde(String path, String demoEsperado) {
        String body = client.get()
                .uri(path)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body).contains("\"demo\"").contains(demoEsperado);
    }

    @Test
    @DisplayName("La landing page lista las 10 preguntas")
    void landingPageListsAllQuestions() {
        String html = client.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(html).isNotNull();
        assertThat(html).contains("ArrayList vs LinkedList")
                .contains("REST vs SOAP")
                .contains("Merge vs rebase")
                .contains("Pirámide de testing")
                .contains("Ordenar un HashSet")
                .contains("Jerarquía de excepciones")
                .contains("Identificar problemas de rendimiento")
                .contains("Datos enormes en Redis")
                .contains("AOP y su mayor trampa")
                .contains("Limitaciones de GET");
    }

    @Test
    @DisplayName("El round-trip SOAP funciona de extremo a extremo")
    void soapRoundTripEndToEnd() {
        String body = client.get()
                .uri("/q02/soap-roundtrip?name=Ana")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).contains("greetResponse").contains("Ana");
    }

    @Test
    @DisplayName("La demo de la P8 hace un round-trip de un blob")
    void blobRoundTripEndToEnd() {
        String id = "e2e-blob";
        String guardado = client.post()
                .uri("/q08/blob/" + id)
                .body("contenido de prueba para el round-trip")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(guardado).contains(id).contains("chunkCount");

        String leido = client.get()
                .uri("/q08/blob/" + id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(leido).contains("round-trip");
    }

    @Test
    @DisplayName("La demo de la P10 marca el GET que muta estado como anti-patrón")
    void unsafeGetIsLabelled() {
        String body = client.get()
                .uri("/q10/unsafe-increment")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).containsIgnoringCase("anti-patrón");
    }
}
