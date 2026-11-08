package com.dsklabs.epam.tecnicaltest.q02webservices.soap;

import com.dsklabs.epam.tecnicaltest.q02webservices.Q02Namespaces;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Petición SOAP (elemento raíz {@code greetRequest}).
 *
 * <p>Es el equivalente JAXB de lo que en REST sería el body JSON. Estas clases son las que
 * {@code wsimport} convertiría en stubs al procesar el WSDL, y las que el marshaller de
 * Spring-WS usa para (de)serializar el sobre SOAP.
 */
@XmlRootElement(name = Q02Namespaces.REQUEST_ROOT, namespace = Q02Namespaces.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class GreetingRequest {

    @XmlElement(name = "name", namespace = Q02Namespaces.NS)
    private String name;

    public GreetingRequest() {
    }

    public GreetingRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "GreetingRequest{name='" + name + "'}";
    }
}
