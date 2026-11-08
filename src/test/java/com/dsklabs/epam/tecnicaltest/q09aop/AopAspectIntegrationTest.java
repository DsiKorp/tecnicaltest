package com.dsklabs.epam.tecnicaltest.q09aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Capa de INTEGRACIÓN de la pregunta 9: los tres aspects contra el contenedor real.
 *
 * <p>Es un test de <b>componente</b> ({@code @SpringBootTest} + MockMvc), no unitario: los
 * aspects solo existen cuando hay un contenedor que crea el proxy, y lo que se quiere
 * comprobar es precisamente eso (que el proxy intercepta, que el advice se dispara y que la
 * auto-invocación se lo salta).
 *
 * <p>Con {@code @AutoConfigureMockMvc} el contexto web es simulado pero <b>completo</b>: la
 * petición pasa por el {@code DispatcherServlet} de verdad, así que el
 * {@code RequestContextHolder} queda poblado y el aspect de autorización puede leer la
 * cabecera {@code X-Role}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AopAspectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CachingAspect cachingAspect;

    @Autowired
    private AuditAspect auditAspect;

    @BeforeEach
    void limpioLosContadores() {
        cachingAspect.limpiar();
        auditAspect.reiniciarContador();
    }

    @Test
    @DisplayName("El endpoint auditado responde 200 y devuelve el pedido")
    void endpointAuditadoRespondeOk() throws Exception {
        mockMvc.perform(get("/q09/order").param("id", "42").header("X-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demo").value("q09-aop"))
                .andExpect(jsonPath("$.data.orden.id").value("42"))
                .andExpect(jsonPath("$.data.orden.estado").value("PAGADO"));
    }

    @Test
    @DisplayName("Sin la cabecera X-Role el aspect de autorización responde 403")
    void sinRolLaPeticionEsRechazada() throws Exception {
        mockMvc.perform(get("/q09/order").param("id", "42"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Un rol no permitido también se rechaza con 403")
    void conRolNoPermitidoLaPeticionEsRechazada() throws Exception {
        mockMvc.perform(get("/q09/order").param("id", "42").header("X-Role", "INVITADO"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La segunda llamada idéntica la resuelve el aspect de caché (acierto)")
    void laSegundaLlamadaEsUnAciertoDeCache() throws Exception {
        mockMvc.perform(get("/q09/order").param("id", "42").header("X-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cache.ultimoResultado").value("MISS"))
                .andExpect(jsonPath("$.data.cache.fallos").value(1));

        mockMvc.perform(get("/q09/order").param("id", "42").header("X-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cache.ultimoResultado").value("HIT"))
                .andExpect(jsonPath("$.data.cache.aciertos").value(1));
    }

    @Test
    @DisplayName("La auto-invocación no dispara el aspect: solo lo hace la llamada por el proxy")
    void laAutoInvocacionNoDisparaElAspect() throws Exception {
        mockMvc.perform(get("/q09/self-invocation").header("X-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aspectoDisparoEnLaLlamadaInterna").value(false))
                .andExpect(jsonPath("$.data.aspectoDisparoEnLaLlamadaPorProxy").value(true))
                // Tras la petición el contador del aspect es exactamente el que reporta la
                // respuesta: durante la petición solo ha saltado el advice de la llamada externa.
                .andExpect(jsonPath("$.data.invocacionesAuditoriaTrasLaLlamadaPorProxy")
                        .value(auditAspect.invocaciones()));
    }

    @Test
    @DisplayName("El bean inyectado es un proxy CGLIB, no la clase que escribimos")
    void elBeanInyectadoEsUnProxy() throws Exception {
        String respuesta = mockMvc.perform(get("/q09/proxy-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.esProxyDeAop").value(true))
                .andExpect(jsonPath("$.data.claseObjetivoReal").value(OrderQueryService.class.getName()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(respuesta).contains("$$SpringCGLIB$$");
    }

    @Test
    @DisplayName("La medición de sobrecarga devuelve las tres mediciones y el ratio")
    void laMedicionDeSobrecargaSeSirve() throws Exception {
        mockMvc.perform(get("/q09/overhead").param("iterations", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.iteraciones").value(2000))
                .andExpect(jsonPath("$.data.directo.scenario").value("directo (objeto real, sin proxy)"))
                .andExpect(jsonPath("$.data.ratioProxyConAdviceSobreDirecto").isNumber());
    }
}
