package com.dsklabs.epam.tecnicaltest.q04testing;

import com.dsklabs.epam.tecnicaltest.support.web.ApiResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la pregunta 4 ("define la pirámide de testing y sus capas").
 *
 * <ul>
 *   <li>{@code GET /q04/pyramid} — el inventario de capas con las clases de test reales de este
 *       proyecto.</li>
 *   <li>{@code GET /q04/load?requests=200&concurrency=20} — el bloque lateral de carga: una
 *       prueba de carga real contra la aplicación en marcha.</li>
 * </ul>
 */
@RestController
@RequestMapping("/q04")
public class TestingPyramidController {

    private final LoadTestRunner loadTestRunner;

    public TestingPyramidController(LoadTestRunner loadTestRunner) {
        this.loadTestRunner = loadTestRunner;
    }

    /**
     * La pirámide completa: capas, qué cubre cada una y qué clases las implementan aquí.
     */
    @GetMapping("/pyramid")
    public ApiResponse<Map<String, Object>> pyramid() {
        List<Map<String, Object>> inventory = TestLayer.inventario();

        Map<String, Object> junior = new LinkedHashMap<>();
        junior.put("capas", List.of("UNIT", "COMPONENT", "INTEGRATION", "E2E"));
        junior.put("descripcion",
                "La pirámide clásica de tres capas: unitarias abajo, integración en medio y UI "
                        + "(E2E) en la punta.");

        Map<String, Object> senior = new LinkedHashMap<>();
        senior.put("capas", List.of("LOAD", "PERFORMANCE"));
        senior.put("descripcion",
                "El bloque adicional que se le pide a un senior: un bloque más pequeño, al lado de "
                        + "la pirámide, con las pruebas de carga y de rendimiento. No miden si el "
                        + "código es correcto, sino cómo se comporta el sistema bajo presión.");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("capas", inventory);
        data.put("perfilJunior", junior);
        data.put("perfilSenior", senior);
        data.put("principio",
                "La pirámide es una consecuencia de la VELOCIDAD: un test que tarda 10 ms se "
                        + "escribe mil veces y se ejecuta en cada commit; uno que tarda 10 minutos "
                        + "se escribe tres veces y se ejecuta de noche. El objetivo no es "
                        + "'tener tests', es maximizar el número de fallos detectados por segundo "
                        + "de tiempo de ejecución y de integración.");
        data.put("explicacion",
                "La pirámide tiene muchos tests unitarios en la base (rápidos, sin Spring, sin "
                        + "red), menos de integración en el centro (componentes reales hablando "
                        + "por HTTP o SOAP) y muy pocos end-to-end en la punta (el flujo completo, "
                        + "lentos y frágiles). Al lado, para el perfil senior, está el bloque "
                        + "pequeño de carga y rendimiento. Debajo de la pirámide, un bloque "
                        + "transversal de calidad: cobertura de código, análisis estático "
                        + "(SonarQube, Checkstyle, SpotBugs) y pruebas de seguridad y accesibilidad.");

        return ApiResponse.of("q04-testing",
                "La pirámide de testing: muchas unitarias abajo, pocas E2E arriba y el bloque "
                        + "de carga y rendimiento al lado",
                data);
    }

    /**
     * Ejecuta una prueba de carga contra la aplicación en marcha.
     */
    @GetMapping("/load")
    public ApiResponse<Map<String, Object>> load(
            @RequestParam(defaultValue = "200") int requests,
            @RequestParam(defaultValue = "20") int concurrency,
            @RequestParam(required = false) String path) {

        Map<String, Object> results = loadTestRunner.run(path, requests, concurrency);

        return ApiResponse.of("q04-testing",
                "Prueba de carga real contra la aplicación: " + results.get("peticionesTotales")
                        + " peticiones con " + results.get("concurrencia") + " en vuelo",
                results);
    }
}
