package com.dsklabs.epam.tecnicaltest.q02webservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Capa de INTEGRACIÓN de la pregunta 2.
 *
 * <p>Este test es la garantía de que la demo SOAP funciona de verdad: levanta el contenedor
 * Tomcat real, llama al endpoint SOAP por HTTP, comprueba el sobre de respuesta y verifica que
 * el WSDL se sirve. Si el stack SOAP se rompe, el build se pone rojo ANTES de la entrevista,
 * no durante la demo.
 *
 * <p>Usa {@link RestTestClient}, la API de test HTTP de Spring Framework 7 / Boot 4
 * (sustituye a {@code TestRestTemplate} y a {@code MockMvc} para llamadas sobre HTTP real).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class Q02WebServicesIntegrationTest {

    @Autowired
    private RestTestClient client;

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("El endpoint REST devuelve el saludo en JSON")
    void restGreeting() {
        client.get()
                .uri("/q02/rest/greeting?name=Ana")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.demo").isEqualTo("q02-rest")
                .jsonPath("$.data.greeting").value(String.class, greeting -> assertThat(greeting).contains("Ana"));
    }

    @Test
    @DisplayName("El WSDL se sirve y describe la operación greet")
    void wsdlIsServed() {
        String wsdl = client.get()
                .uri("/q02/wsdl")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(wsdl).isNotNull();
        assertThat(wsdl).contains("wsdl:definitions");
        assertThat(wsdl).contains("Q02GreetingService");
        assertThat(wsdl).contains("greetRequest");
        assertThat(wsdl).contains("greetResponse");
        assertThat(wsdl).contains(Q02Namespaces.NS);
    }

    @Test
    @DisplayName("Round-trip SOAP real: el endpoint devuelve el sobre con el saludo")
    void soapRoundTrip() throws Exception {
        // El cliente SOAP habla HTTP crudo: necesita la URL absoluta con el puerto real.
        String endpoint = "http://localhost:" + port + Q02Namespaces.SOAP_PATH;

        SoapGreetingClient soapClient = new SoapGreetingClient(endpoint);
        SoapGreetingClient.SoapRoundTrip trip = soapClient.call("Ana");

        assertThat(trip.statusCode()).isEqualTo(200);
        assertThat(trip.requestXml()).contains("greetRequest").contains("Ana");
        assertThat(trip.responseXml()).contains("Envelope").contains("Body");
        assertThat(trip.greeting()).isNotNull().contains("Ana");
    }

    @Test
    @DisplayName("El endpoint SOAP responde a una petición enviada por el propio endpoint REST")
    void soapRoundTripThroughController() throws Exception {
        client.get()
                .uri("/q02/soap-roundtrip?name=Ana")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.demo").isEqualTo("q02-soap")
                .jsonPath("$.data.responseEnvelope").value(String.class,
                        envelope -> assertThat(envelope).contains("greetResponse"));
    }

    @Test
    @DisplayName("El documento OpenAPI se sirve y es parseable como YAML")
    void openApiDocument() {
        String yaml = client.get()
                .uri("/q02/openapi.yaml")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(yaml).contains("openapi: 3.0.3");
        assertThat(yaml).contains("/q02/rest/greeting");

        client.get()
                .uri("/q02/openapi")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.openapi").isEqualTo("3.0.3")
                .jsonPath("$.paths['/q02/rest/greeting'].get.operationId").isEqualTo("getGreeting");
    }

    @Test
    @DisplayName("La comparación WSDL vs OpenAPI está disponible")
    void compareContracts() {
        client.get()
                .uri("/q02/compare")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.demo").isEqualTo("q02-compare")
                .jsonPath("$.data.soap.contrato").isEqualTo("WSDL 1.1")
                .jsonPath("$.data.rest.contrato").isEqualTo("OpenAPI 3.0");
    }
}
