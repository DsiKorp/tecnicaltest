package com.dsklabs.epam.tecnicaltest.q02webservices.rest;

import com.dsklabs.epam.tecnicaltest.q02webservices.Q02Namespaces;
import com.dsklabs.epam.tecnicaltest.support.web.ApiResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lado REST del mismo servicio de saludo.
 *
 * <p>Mismo contrato de negocio, mismo payload, transporte distinto: la respuesta es JSON y el
 * contrato es el documento OpenAPI de {@code /q02/openapi.yaml}.
 */
@RestController
@RequestMapping("/q02/rest")
public class GreetingRestController {

    @GetMapping("/greeting")
    public ApiResponse<GreetingPayload> greeting(@RequestParam(defaultValue = "mundo") String name) {
        return ApiResponse.of(
                "q02-rest",
                "Mismo servicio, transporte REST: JSON sobre HTTP",
                new GreetingPayload(
                        "¡Hola, " + name + "! (servicio REST: JSON sobre HTTP, contrato OpenAPI)",
                        "REST / JSON",
                        Q02Namespaces.NS));
    }

    /**
     * @param greeting mensaje
     * @param transport transporte usado
     * @param namespace target namespace compartido por WSDL y OpenAPI
     */
    public record GreetingPayload(String greeting, String transport, String namespace) {
    }
}
