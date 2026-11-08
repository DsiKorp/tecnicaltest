package com.dsklabs.epam.tecnicaltest.q02webservices;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;

/**
 * Configuración del stack SOAP (Spring-WS).
 *
 * <p>{@code @EnableWs} registra el mapping por {@code @PayloadRoot}, el adaptador de métodos y
 * los resolvers de excepciones SOAP. El {@link MessageDispatcherServlet} es el servlet que
 * atiende las peticiones SOAP; se mapea en {@code /soap/*}.
 *
 * <p>El WSDL se sirve desde Spring MVC en {@code /q02/wsdl} (ver {@code WsdlController}) para
 * que convivan sin colisión de rutas: el servlet de Spring-WS y el DispatcherServlet de MVC son
 * dos mappings distintos.
 */
@Configuration
@EnableWs
public class Q02WebServicesConfig {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext context) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(context);
        // Reescribe la location del WSDL con el host y puerto reales de la petición.
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/soap/*");
    }
}
