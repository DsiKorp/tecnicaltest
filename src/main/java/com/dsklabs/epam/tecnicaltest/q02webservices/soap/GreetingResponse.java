package com.dsklabs.epam.tecnicaltest.q02webservices.soap;

import com.dsklabs.epam.tecnicaltest.q02webservices.Q02Namespaces;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Respuesta SOAP (elemento raíz {@code greetResponse}). */
@XmlRootElement(name = Q02Namespaces.RESPONSE_ROOT, namespace = Q02Namespaces.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class GreetingResponse {

    @XmlElement(name = "greeting", namespace = Q02Namespaces.NS)
    private String greeting;

    @XmlElement(name = "transport", namespace = Q02Namespaces.NS)
    private String transport;

    public GreetingResponse() {
    }

    public GreetingResponse(String greeting, String transport) {
        this.greeting = greeting;
        this.transport = transport;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    @Override
    public String toString() {
        return "GreetingResponse{greeting='" + greeting + "', transport='" + transport + "'}";
    }
}
