package com.dsklabs.epam.tecnicaltest.q09aop;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.dsklabs.epam.tecnicaltest.q09aop.OrderQueryService.Pedido;
import com.dsklabs.epam.tecnicaltest.q09aop.OrderQueryService.ResultadoAutoInvocacion;
import com.dsklabs.epam.tecnicaltest.support.benchmark.BenchmarkHarness;
import com.dsklabs.epam.tecnicaltest.support.benchmark.BenchmarkResult;
import com.dsklabs.epam.tecnicaltest.support.web.ApiResponse;

import org.springframework.aop.support.AopUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la pregunta 9 (AOP: definición y su mayor trampa).
 *
 * <ul>
 *   <li>{@code GET /q09/order} — un método con los tres aspects: autorización, auditoría y
 *       caché. Muestra el estado del caché (aciertos/fallos) y el contador de auditoría.</li>
 *   <li>{@code GET /q09/overhead} — la trampa medida: llamada directa contra llamada proxificada,
 *       con el harness de benchmarks del proyecto.</li>
 *   <li>{@code GET /q09/proxy-info} — qué es realmente el objeto que te inyecta el contenedor.</li>
 *   <li>{@code GET /q09/self-invocation} — la demo más valuable: el aspect no se dispara en la
 *       llamada interna ({@code this.metodo()}).</li>
 *   <li>{@code GET /q09/search} — un método solo auditado, para ver el advice sin efectos
 *       de negocio.</li>
 * </ul>
 *
 * <p>Los endpoints que llaman a {@link OrderQueryService#findOrder(String)} exigen la cabecera
 * {@code X-Role} con un rol válido: el aspect de autorización responde 403 si falta.
 */
@RestController
@RequestMapping("/q09")
public class AopController {

    private static final String DEMO = "q09-aop";

    private final OrderQueryService orderService;
    private final AopOverheadService overheadService;
    private final AuditAspect auditAspect;
    private final CachingAspect cachingAspect;

    public AopController(OrderQueryService orderService, AopOverheadService overheadService,
                         AuditAspect auditAspect, CachingAspect cachingAspect) {
        this.orderService = orderService;
        this.overheadService = overheadService;
        this.auditAspect = auditAspect;
        this.cachingAspect = cachingAspect;
    }

    /**
     * Un método de negocio con los tres aspects.
     *
     * @param id identificador del pedido
     * @return el pedido y el estado de la caché
     */
    @GetMapping("/order")
    public ApiResponse<Map<String, Object>> order(@RequestParam(defaultValue = "42") String id) {

        Pedido pedido = orderService.findOrder(id);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orden", pedido);
        data.put("cache", cachingAspect.estado());
        data.put("invocacionesAuditoria", auditAspect.invocaciones());
        data.put("nota",
                "La primera llamada a un id nuevo es un fallo de caché (MISS) y la segunda es un "
                        + "acierto (HIT): por eso el contador de aciertos sube sin que el método "
                        + "vuelva a ejecutarse.");

        return ApiResponse.of(DEMO, "Auditoría, autorización y caché sobre el mismo método", data);
    }

    /**
     * Búsqueda simple: solo auditoría, sin autorización ni caché.
     *
     * @param term texto de búsqueda
     * @return los pedidos encontrados y el contador de auditoría
     */
    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> search(@RequestParam(defaultValue = "42") String term) {

        List<Pedido> pedidos = orderService.search(term);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pedidos", pedidos);
        data.put("invocacionesAuditoria", auditAspect.invocaciones());
        data.put("nota",
                "Esta operación solo está anotada con @Audited: el aspect transversal se aplica "
                        + "sin tocar una línea del método de negocio.");

        return ApiResponse.of(DEMO, "Un aspecto transversal sin efectos sobre el negocio", data);
    }

    /**
     * La trampa, medida: llamada directa contra llamada a través del proxy.
     *
     * @param iterations número de iteraciones de la medición
     * @return las tres mediciones y sus ratios
     */
    @GetMapping("/overhead")
    public ApiResponse<Map<String, Object>> overhead(@RequestParam(defaultValue = "100000") int iterations) {

        int seguras = Math.max(1_000, Math.min(iterations, 500_000));
        AopOverheadService objetoReal = overheadService.sinProxy();

        // Sumidero: sin escribir el resultado, la JIT podría eliminar el trabajo medido.
        AtomicLong sumidero = new AtomicLong();

        BenchmarkResult directo;
        BenchmarkResult proxySinAdvice;
        BenchmarkResult proxyConAdvice;

        // Se silencia la traza de auditoría: con 100.000 llamadas, el log dominaría la
        // medición y no se vería el coste real del proxy.
        auditAspect.silenciar(true);
        try {
            directo = BenchmarkHarness.measure("directo (objeto real, sin proxy)", seguras, 1,
                    () -> sumidero.set(objetoReal.direct()));
            proxySinAdvice = BenchmarkHarness.measure("proxy sin advice (reflexión)", seguras, 1,
                    () -> sumidero.set(overheadService.direct()));
            proxyConAdvice = BenchmarkHarness.measure("proxy con advice de auditoría", seguras, 1,
                    () -> sumidero.set(overheadService.proxied()));
        } finally {
            auditAspect.silenciar(false);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("iteraciones", seguras);
        data.put("directo", directo);
        data.put("proxySinAdvice", proxySinAdvice);
        data.put("proxyConAdvice", proxyConAdvice);
        data.put("medianaMsDirecto", directo.medianMs());
        data.put("medianaMsProxySinAdvice", proxySinAdvice.medianMs());
        data.put("medianaMsProxyConAdvice", proxyConAdvice.medianMs());
        data.put("ratioProxySinAdviceSobreDirecto", ratio(proxySinAdvice, directo));
        data.put("ratioProxyConAdviceSobreDirecto", ratio(proxyConAdvice, directo));
        data.put("nota",
                "Los tres caminos ejecutan el mismo trabajo. La diferencia es el AOP: el proxy "
                        + "CGLIB intercepta también los métodos sin aspect (no encuentra ninguno "
                        + "que coincida y aun así los invoca por reflexión) y el advice añade la "
                        + "creación del join point y la invocación del advice. Con trabajo de "
                        + "negocio real (una consulta a base de datos) ese sobrecoste es ruido "
                        + "comparado con la latencia de la operación.");

        return ApiResponse.of(DEMO, "El coste medido de meter un proxy AOP por medio", data);
    }

    /**
     * Qué es realmente el objeto que te inyecta el contenedor.
     *
     * @return nombre de la clase en tiempo de ejecución, clase objetivo y explicación
     */
    @GetMapping("/proxy-info")
    public ApiResponse<Map<String, Object>> proxyInfo() {

        Class<?> claseRuntime = orderService.getClass();
        Class<?> claseObjetivo = AopUtils.getTargetClass(orderService);

        List<String> interfaces = Arrays.stream(claseRuntime.getInterfaces())
                .map(Class::getName)
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("claseEnTiempoDeEjecucion", claseRuntime.getName());
        data.put("esProxyDeAop", AopUtils.isAopProxy(orderService));
        data.put("claseSuperclase", claseRuntime.getSuperclass() == null
                ? null
                : claseRuntime.getSuperclass().getName());
        data.put("interfaces", interfaces);
        data.put("claseObjetivoReal", claseObjetivo.getName());
        data.put("pistaDelProxy",
                "El nombre de la clase en tiempo de ejecución contiene el fragmento '$$SpringCGLIB$$': "
                        + "esa es la firma de un proxy generado por CGLIB.");
        data.put("explicacion",
                "El contenedor no te inyecta OrderQueryService: te inyecta un proxy generado por "
                        + "CGLIB (el nombre de la clase en tiempo de ejecución lleva '$$SpringCGLIB$$'). "
                        + "Cuando llamas a findOrder(...) no estás hablando con el objeto que "
                        + "creaste: estás hablando con el proxy, que ejecuta los aspects y luego "
                        + "delega en el objeto real. Por eso 'this' dentro de la clase apunta al "
                        + "objeto real y se salta los aspects (auto-invocación).");

        return ApiResponse.of(DEMO, "El objeto inyectado es un proxy, no el bean que escribiste", data);
    }

    /**
     * La demo más valuable: el aspect no se dispara en la llamada interna.
     *
     * <p>Se mide el contador de auditoría del aspect en tres momentos: antes de la llamada
     * interna, después de la llamada interna y después de la llamada que entra por el proxy. Si
     * el aspect se ejecutase en las dos, el contador subiría dos veces.
     *
     * @return las dos llamadas y las tres lecturas del contador
     */
    @GetMapping("/self-invocation")
    public ApiResponse<Map<String, Object>> selfInvocation() {

        long antes = auditAspect.invocaciones();

        // Vía interna: el propio bean llama a this.findOrder("42") -> no hay proxy -> no hay aspect.
        ResultadoAutoInvocacion interno = orderService.selfInvocationDemo();
        long trasLlamadaInterna = auditAspect.invocaciones();

        // Vía externa: la misma llamada, pero entrando por el proxy -> los aspects se ejecutan.
        Pedido porProxy = orderService.findOrder("42");
        long trasLlamadaPorProxy = auditAspect.invocaciones();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("llamadaPorProxy", porProxy);
        data.put("llamadaInterna", interno.pedidoInterno());
        data.put("invocacionesAuditoriaAntes", antes);
        data.put("invocacionesAuditoriaTrasLaLlamadaInterna", trasLlamadaInterna);
        data.put("invocacionesAuditoriaTrasLaLlamadaPorProxy", trasLlamadaPorProxy);
        data.put("aspectoDisparoEnLaLlamadaInterna", trasLlamadaInterna > antes);
        data.put("aspectoDisparoEnLaLlamadaPorProxy", trasLlamadaPorProxy > trasLlamadaInterna);
        data.put("explicacion", interno.explicacion());
        data.put("consecuencia",
                "El mismo problema afecta a la autorización: findOrder está anotado con @Authorized, "
                        + "pero la llamada interna se ha resuelto sin comprobar el rol. Un aspect de "
                        + "seguridad no protege frente a las llamadas internas del propio bean.");

        return ApiResponse.of(DEMO, "Auto-invocación: el aspect no se dispara en la llamada interna", data);
    }

    /** Ratio entre dos mediciones (mediana del numerador entre la del denominador). */
    private double ratio(BenchmarkResult numerador, BenchmarkResult denominador) {
        if (denominador.medianNs() == 0) {
            return 0;
        }
        return Math.round((numerador.medianNs() * 100.0 / denominador.medianNs())) / 100.0;
    }
}
