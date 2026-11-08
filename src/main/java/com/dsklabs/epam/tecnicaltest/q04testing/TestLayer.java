package com.dsklabs.epam.tecnicaltest.q04testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La pirámide de testing del proyecto, capa por capa.
 *
 * <p><b>La respuesta a la pregunta 4 ("define la pirámide de testing y sus capas"):</b>
 *
 * <p>Para un perfil junior la pirámide tiene tres capas, de abajo arriba:
 *
 * <ol>
 *   <li><b>Unitarias</b> — una clase o un método, sin Spring ni red. Miles, rapidísimas.</li>
 *   <li><b>Integración</b> — varios componentes reales hablando entre sí (HTTP, SOAP, base de
 *       datos). Decenas.</li>
 *   <li><b>UI / E2E</b> — el flujo completo, tal y como lo ve el usuario. Pocas, lentas y
 *       frágiles.</li>
 * </ol>
 *
 * <p>Para un perfil senior hay un <b>bloque lateral</b>, más pequeño, que no está en la pirámide
 * porque mide el sistema en producción y no la corrección del código:
 *
 * <ul>
 *   <li><b>Carga</b> — cuántos usuarios simultáneos aguanta y con qué latencia.</li>
 *   <li><b>Rendimiento</b> — cuánto cuesta cada operación concreta.</li>
 * </ul>
 *
 * <p>El <b>principio</b> que hay que saber decir: la pirámide es una consecuencia de la
 * <b>velocidad</b>. Un test que tarda 10 ms se escribe mil veces; uno que tarda 10 minutos se
 * escribe tres. Por eso las capas caras (UI, carga) son pocas y las baratas (unitarias) son
 * muchas: se busca el máximo número de fallos detectados por segundo de tiempo de ejecución.
 */
public enum TestLayer {

    /** Capa base: lógica aislada, sin framework ni red. */
    UNIT("Unitarias",
            "Prueban una unidad de código aislada (una clase o un método) sin Spring, sin red y "
                    + "sin base de datos. Son la base de la pirámide: miles de tests, se ejecutan en "
                    + "milisegundos y localizan el fallo al instante.",
            List.of("SimpleArrayListTest", "SimpleLinkedListTest", "HashSetOrderingServiceTest",
                    "OrderServiceTest", "BlobCodecTest", "InMemoryChunkedBlobStoreTest",
                    "GetLimitsServiceTest")),

    /** Capa intermedia: un componente con sus dependencias reales, dentro del mismo proceso. */
    COMPONENT("Componente",
            "Prueban una pieza del sistema con sus dependencias reales pero sin salir del proceso: "
                    + "un controller con su service y su serialización, un aspect con su proxy de "
                    + "Spring. Es la capa que detecta problemas de wiring (inyección, aspect, "
                    + "serialización) sin el coste de levantar todo.",
            List.of("WebLayerComponentTest", "AopAspectIntegrationTest")),

    /** Capa de integración: varios componentes reales hablando por el cable. */
    INTEGRATION("Integración",
            "Prueban la interacción entre componentes reales a través de sus fronteras: HTTP, "
                    + "SOAP/WSDL, base de datos, Redis. Verifican que el contrato se cumple de "
                    + "verdad y no solo que las clases encajan entre sí.",
            List.of("Q02WebServicesIntegrationTest")),

    /** Punta de la pirámide: el flujo completo de la aplicación. */
    E2E("End-to-end (UI / aceptación)",
            "Recorren el flujo completo de la aplicación de principio a fin, tal y como lo "
                    + "percibe el usuario. Son las más lentas, las más frágiles (cualquier cambio en "
                    + "la UI las rompe) y por eso son las pocas de la punta.",
            List.of("DemosEndToEndTest")),

    /** Bloque lateral (senior): comportamiento bajo carga. */
    LOAD("Carga (load)",
            "Miden el comportamiento del sistema bajo carga: cuántos usuarios concurrentes aguanta, "
                    + "cuál es la latencia y cuántos errores aparecen. No viven en la pirámide "
                    + "porque miden el sistema en marcha, no la corrección del código. En este "
                    + "proyecto se ejecutan a mano porque son lentos y flaky en CI.",
            List.of("LoadTest")),

    /** Bloque lateral (senior): rendimiento de operaciones concretas. */
    PERFORMANCE("Rendimiento (performance)",
            "Miden el coste de operaciones concretas (benchmarks in-process) con el arnés "
                    + "BenchmarkHarness, que reporta min, mediana y p95. OJO: no es una clase de "
                    + "test JUnit, es una herramienta de medición usada por las demos de Q1, Q7 y Q9.",
            List.of("BenchmarkHarness"));

    private final String displayName;
    private final String description;
    private final List<String> testClasses;

    TestLayer(String displayName, String description, List<String> testClasses) {
        this.displayName = displayName;
        this.description = description;
        this.testClasses = List.copyOf(testClasses);
    }

    /** Nombre legible de la capa. */
    public String displayName() {
        return displayName;
    }

    /** Qué cubre exactamente esta capa, en español. */
    public String description() {
        return description;
    }

    /** Clases de test de este proyecto que implementan la capa. */
    public List<String> testClasses() {
        return testClasses;
    }

    /**
     * El inventario completo de la pirámide.
     *
     * @return una entrada por capa, con su nombre, su descripción y sus clases de test
     */
    public static List<Map<String, Object>> inventario() {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (TestLayer layer : values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("clave", layer.name());
            row.put("capa", layer.displayName);
            row.put("descripcion", layer.description);
            row.put("clasesDeTest", layer.testClasses);
            row.put("cantidadDeClases", layer.testClasses.size());
            rows.add(row);
        }

        return rows;
    }
}
