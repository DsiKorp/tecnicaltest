package com.dsklabs.epam.tecnicaltest.q04testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Capa COMPONENT de la pirámide de testing.
 *
 * <p>Prueba la capa web sin abrir un puerto: {@code MockMvc} invoca los controladores con el
 * contexto real de Spring, pero sin red. Es la capa intermedia: más rápida que la integración
 * (que sí levanta Tomcat) y más fiel que un test unitario.
 *
 * <p>La diferencia con {@code DemosEndToEndTest} es que aquí no hay servidor HTTP: el
 * {@code DispatcherServlet} se invoca en proceso.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebLayerComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("La landing page se sirve en la raíz")
    void landingPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("El endpoint de salud responde")
    void health() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("La demo de la P1 responde con el sobre común")
    void q01() throws Exception {
        mockMvc.perform(get("/q01/internals").param("elements", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demo").value("q01-internals"))
                .andExpect(jsonPath("$.data.SimpleArrayList").exists())
                .andExpect(jsonPath("$.data.SimpleLinkedList").exists());
    }

    @Test
    @DisplayName("La demo de la P2 sirve el WSDL y el OpenAPI")
    void q02() throws Exception {
        mockMvc.perform(get("/q02/wsdl"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.containsString("wsdl:definitions")));

        mockMvc.perform(get("/q02/openapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.containsString("openapi: 3.0.3")));
    }

    @Test
    @DisplayName("La demo de la P5 devuelve el orden de iteración")
    void q05() throws Exception {
        mockMvc.perform(get("/q05/order").param("strategy", "tree").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demo").value("q05-hashset"));
    }

    @Test
    @DisplayName("La demo de la P6 devuelve la jerarquía de excepciones")
    void q06() throws Exception {
        mockMvc.perform(get("/q06/hierarchy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.raiz").value("AppException (sealed)"));
    }

    @Test
    @DisplayName("La demo de la P8 informa del almacén activo")
    void q08() throws Exception {
        mockMvc.perform(get("/q08/store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.almacenActivo").exists());
    }

    @Test
    @DisplayName("La demo de la P10 mide el presupuesto de la URL")
    void q10() throws Exception {
        mockMvc.perform(get("/q10/measure").param("length", "3000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dentroDelPresupuestoPractico").value(false))
                .andExpect(jsonPath("$.data.limiteRealDelContenedorBytes").exists());
    }

    @Test
    @DisplayName("Un endpoint inexistente devuelve 404")
    void notFound() throws Exception {
        mockMvc.perform(get("/no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("La API de OpenAPI responde JSON")
    void openApiJson() throws Exception {
        mockMvc.perform(get("/q02/openapi").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.0.3"));
    }
}
