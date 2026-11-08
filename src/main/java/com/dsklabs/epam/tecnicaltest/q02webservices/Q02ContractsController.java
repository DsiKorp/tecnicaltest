package com.dsklabs.epam.tecnicaltest.q02webservices;

import com.dsklabs.epam.tecnicaltest.support.web.ApiResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.yaml.snakeyaml.Yaml;

/**
 * Expone los dos contratos de la pregunta 2 y los pone lado a lado.
 *
 * <ul>
 *   <li>{@code /q02/wsdl} — el WSDL 1.1 del servicio SOAP (XML).</li>
 *   <li>{@code /q02/openapi.yaml} — el documento OpenAPI 3.0 tal cual (YAML).</li>
 *   <li>{@code /q02/openapi} — el mismo documento en JSON, para pintarlo en el navegador.</li>
 *   <li>{@code /q02/compare} — la comparación WSDL vs OpenAPI (la respuesta de la pregunta).</li>
 *   <li>{@code /q02/soap-roundtrip} — invoca el endpoint SOAP y devuelve el sobre crudo.</li>
 * </ul>
 *
 * <p>El OpenAPI se parsea con SnakeYAML (ya está en el classpath vía {@code spring-boot-starter})
 * y se devuelve como {@code Map}: Spring lo serializa a JSON. Así <b>no importamos Jackson
 * nunca</b> — en Boot 4 la librería es Jackson 3 ({@code tools.jackson}) y acoplar el código
 * al serializador es justo lo que hay que evitar.
 */
@RestController
public class Q02ContractsController {

    private static final String WSDL_RESOURCE = "wsdl/q02-greeting.wsdl";
    private static final String OPENAPI_RESOURCE = "openapi/q02-openapi.yaml";

    /**
     * URL base de la petición en curso.
     *
     * <p>Se construye desde la request y no desde {@code server.port} a propósito: así las URLs
     * que salen en la respuesta son correctas tanto en local (localhost:8080), en los tests
     * (puerto aleatorio) y en AWS (la Elastic IP de la instancia).
     */
    private String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }

    /** El WSDL tal cual: es lo que consume `wsimport` para generar los stubs. */
    @GetMapping(value = "/q02/wsdl", produces = MediaType.APPLICATION_XML_VALUE)
    public String wsdl() throws IOException {
        return readResource(WSDL_RESOURCE);
    }

    /** El documento OpenAPI en YAML, tal cual. */
    @GetMapping(value = "/q02/openapi.yaml", produces = "application/yaml")
    public String openApiYaml() throws IOException {
        return readResource(OPENAPI_RESOURCE);
    }

    /** El mismo documento OpenAPI en JSON, para la UI del navegador. */
    @GetMapping(value = "/q02/openapi", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> openApiJson() throws IOException {
        Yaml yaml = new Yaml();
        Object parsed = yaml.load(readResource(OPENAPI_RESOURCE));
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) map;
            return result;
        }
        return Map.of("error", "El documento OpenAPI no se pudo parsear como YAML");
    }

    /**
     * La comparación: la respuesta directa a "REST vs SOAP".
     */
    @GetMapping("/q02/compare")
    public ApiResponse<Map<String, Object>> compare() {
        Map<String, Object> wsdl = new LinkedHashMap<>();
        wsdl.put("contrato", "WSDL 1.1");
        wsdl.put("url", baseUrl() + Q02Namespaces.WSDL_PATH);
        wsdl.put("formato", "XML");
        wsdl.put("generaCodigo", "wsimport -p <paquete> " + baseUrl() + Q02Namespaces.WSDL_PATH);
        wsdl.put("dondeSeUsa", "Servicios SOAP (SOAP 1.1/1.2, WS-*). Heredado: muchos SOAP ya no tienen WSDL publicly publicado.");
        wsdl.put("ejemplo", "Un WSDL describe operaciones, mensajes, bindings y el contrato de transporte.");

        Map<String, Object> openapi = new LinkedHashMap<>();
        openapi.put("contrato", "OpenAPI 3.0");
        openapi.put("url", baseUrl() + "/q02/openapi.yaml");
        openapi.put("formato", "YAML o JSON");
        openapi.put("generaCodigo", "openapi-generator generate -i " + baseUrl() + "/q02/openapi.yaml");
        openapi.put("dondeSeUsa", "APIs REST/HTTP. Es el estándar de facto: Swagger UI, Postman, gateways, clientes autogenerados.");
        openapi.put("ejemplo", "Un documento OpenAPI describe paths, métodos, parámetros, esquemas y respuestas.");

        Map<String, Object> conclusion = new LinkedHashMap<>();
        conclusion.put("puntoClave",
                "WSDL y OpenAPI son la MISMA idea aplicada a dos mundos: contrato primero, "
                        + "generación de código y documentación a partir del contrato.");
        conclusion.put("diferenciaPractica",
                "SOAP es XML y más verboso, con un binding por operación y varias "
                        + "especificaciones que se solapan; REST es JSON y ligero, usando el "
                        + "propio HTTP como protocolo.");
        conclusion.put("criterioDeDecision",
                "SOAP cuando necesitas WS-Security, interoperabilidad con stacks legacy o "
                        + "contratos muy estables. REST cuando quieres payloads ligeros y "
                        + "que el equipo consuma el contrato con OpenAPI.");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("soap", wsdl);
        data.put("rest", openapi);
        data.put("conclusion", conclusion);

        return ApiResponse.of("q02-compare", "WSDL (SOAP) vs OpenAPI (REST): el mismo concepto", data);
    }

    /**
     * Ejecuta el round-trip SOAP contra el endpoint real y devuelve los sobres crudos.
     *
     * <p>Es la mejor forma de mostrar en la entrevista la diferencia de transporte: el mismo
     * dato, un JSON de 4 líneas frente a un sobre XML.
     */
    @GetMapping("/q02/soap-roundtrip")
    public ApiResponse<Map<String, Object>> soapRoundTrip(@RequestParam(defaultValue = "Ana") String name)
            throws Exception {

        SoapGreetingClient client = new SoapGreetingClient(baseUrl() + Q02Namespaces.SOAP_PATH);
        SoapGreetingClient.SoapRoundTrip trip = client.call(name);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("endpoint", baseUrl() + Q02Namespaces.SOAP_PATH);
        data.put("status", trip.statusCode());
        data.put("requestEnvelope", trip.requestXml());
        data.put("responseEnvelope", trip.responseXml());
        data.put("greeting", trip.greeting());
        data.put("nota",
                "Este es el sobre SOAP tal cual. `wsimport` genera un stub que hace exactamente "
                        + "esto por debajo, pero ocultando el XML.");

        return ApiResponse.of("q02-soap", "Round-trip SOAP real contra /soap/greeting", data);
    }

    private String readResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
