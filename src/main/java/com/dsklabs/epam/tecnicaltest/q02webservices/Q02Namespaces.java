package com.dsklabs.epam.tecnicaltest.q02webservices;

/** Constantes del contrato SOAP de la pregunta 2. Compartidas por endpoint, WSDL y cliente. */
public final class Q02Namespaces {

    /** Target namespace del servicio. */
    public static final String NS = "http://epam.com/q02";

    /** Elemento raíz de la petición SOAP. */
    public static final String REQUEST_ROOT = "greetRequest";

    /** Elemento raíz de la respuesta SOAP. */
    public static final String RESPONSE_ROOT = "greetResponse";

    /** Nombre del servicio en el WSDL. */
    public static final String SERVICE_NAME = "Q02GreetingService";

    /** Nombre del portType en el WSDL. */
    public static final String PORT_TYPE_NAME = "Q02GreetingPort";

    /** Ruta donde se publica el endpoint SOAP (servlet de Spring-WS). */
    public static final String SOAP_PATH = "/soap/greeting";

    /** Ruta donde se sirve el WSDL (controller de Spring MVC). */
    public static final String WSDL_PATH = "/q02/wsdl";

    private Q02Namespaces() {
    }
}
