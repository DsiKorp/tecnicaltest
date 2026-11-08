package com.dsklabs.epam.tecnicaltest.q02webservices.soap;

import com.dsklabs.epam.tecnicaltest.q02webservices.Q02Namespaces;

import org.springframework.stereotype.Component;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/**
 * Endpoint SOAP real, publicado por el {@code MessageDispatcherServlet} de Spring-WS.
 *
 * <p><b>Por qué {@code @PayloadRoot} y no {@code @WebService}:</b> Spring-WS 4+ es el enfoque
 * idiomático actual (el de JAX-WS clásico es heredado). El mapping no se hace por URL sino por
 * el elemento raíz del sobre SOAP: por eso el mismo endpoint atiende cualquier ruta bajo
 * {@code /soap/*}. El WSDL se publica aparte y describe este mismo contrato.
 *
 * <p>El binding lo hace {@code XmlRootElementPayloadMethodProcessor} (activo porque hay JAXB en
 * el classpath): enlaza el elemento raíz del sobre con el parámetro cuyo tipo lleva
 * {@code @XmlRootElement} con el mismo nombre y namespace. El mismo processor serializa el
 * valor de retorno, de modo que la respuesta sale con la raíz {@code greetResponse}.
 *
 * <p><b>Tres anotaciones son obligatorias</b> y cada una falla de forma distinta:
 * <ul>
 *   <li>{@code @Endpoint} en la clase — sin ella el mapping no registra el bean y la
 *       petición responde 404 "No endpoint mapping found".</li>
 *   <li>{@code @RequestPayload} en el parámetro — sin ella ningún
 *       {@code MethodArgumentResolver} admite el parámetro.</li>
 *   <li>{@code @ResponsePayload} en el método — sin ella ningún
 *       {@code MethodReturnValueHandler} admite el tipo de retorno.</li>
 * </ul>
 * Si falta cualquiera de las dos últimas, el sobre SOAP responde con el fault
 * "No adapter for endpoint [...]".
 */
@Component
@Endpoint
public class GreetingSoapEndpoint {

    /**
     * Operación SOAP "greet".
     *
     * @param request petición deserializada desde el sobre SOAP
     * @return respuesta que Spring-WS serializa dentro de un sobre SOAP 1.1
     */
    @PayloadRoot(localPart = Q02Namespaces.REQUEST_ROOT, namespace = Q02Namespaces.NS)
    @ResponsePayload
    public GreetingResponse greet(@RequestPayload GreetingRequest request) {
        String name = request.getName() == null ? "mundo" : request.getName();
        return new GreetingResponse(
                "¡Hola, " + name + "! (servicio SOAP: XML sobre HTTP, contrato WSDL)",
                "SOAP 1.1 / document-literal");
    }
}
