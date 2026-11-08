package com.dsklabs.epam.tecnicaltest.q02webservices;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Cliente SOAP que habla el protocolo "a pelo": construye el sobre, lo manda por HTTP y lee el
 * sobre de respuesta.
 *
 * <p><b>Por qué a pelo y no con un stub generado:</b> en la entrevista interesa <b>enseñar el
 * sobre</b>. Un stub de {@code wsimport} abstrae el XML y esconde justo lo que queremos
 * comparar con el JSON de REST. El comando para generar el stub real está en el README y en el
 * propio WSDL:
 *
 * <pre>{@code
 * wsimport -p com.dsklabs.epam.tecnicaltest.q02webservices.client \
 *          http://localhost:8080/q02/wsdl
 * }</pre>
 */
public final class SoapGreetingClient {

    private static final String SOAP_ENV_NS = "http://schemas.xmlsoap.org/soap/envelope/";

    private final HttpClient httpClient;
    private final String endpoint;

    public SoapGreetingClient(String endpoint) {
        this(endpoint, Duration.ofSeconds(10));
    }

    public SoapGreetingClient(String endpoint, Duration timeout) {
        this.endpoint = endpoint;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    /** Construye el sobre SOAP de una petición document/literal. */
    public String buildEnvelope(String name) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <ns:greetRequest xmlns:ns="%s">
                      <ns:name>%s</ns:name>
                    </ns:greetRequest>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(Q02Namespaces.NS, name);
    }

    /** Envía el sobre y devuelve la respuesta cruda (el sobre SOAP completo). */
    public SoapRoundTrip call(String name) throws Exception {
        String requestXml = buildEnvelope(name);

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(requestXml))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return new SoapRoundTrip(requestXml, response.statusCode(), response.body(), extractGreeting(response.body()));
    }

    /** Extrae el valor de {@code greeting} del sobre de respuesta con DOM. */
    private String extractGreeting(String responseXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // El contenido viene de nuestro propio servicio, pero desactivar DTDs es buena
            // práctica por defecto ante XML externo.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(responseXml)));
            NodeList nodes = document.getElementsByTagNameNS(Q02Namespaces.NS, "greeting");
            return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param requestXml  sobre enviado
     * @param statusCode  código HTTP
     * @param responseXml sobre recibido
     * @param greeting    valor extraído del elemento {@code greeting}
     */
    public record SoapRoundTrip(String requestXml, int statusCode, String responseXml, String greeting) {
    }
}
