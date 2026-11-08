package com.dsklabs.epam.tecnicaltest.q03gitops;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dsklabs.epam.tecnicaltest.support.web.ApiResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la pregunta 3 (merge vs rebase).
 *
 * <p>Esta pregunta no es de Java: es de Git. La demo se apoya en dos piezas:
 *
 * <ul>
 *   <li>{@code GET /q03/summary} — la respuesta conceptual en JSON, para tenerla siempre a mano.</li>
 *   <li>{@code GET /q03/transcript} — la transcripción real de la demo, generada por
 *       {@code scripts/git-merge-vs-rebase.sh}. Si el script todavía no se ha ejecutado, el
 *       endpoint lo dice con claridad en lugar de devolver un 500.</li>
 * </ul>
 *
 * <p>La transcripción vive en {@code target/git-demo/transcript.txt} (propiedad
 * {@code epam.q03.transcript-path}).
 */
@RestController
@RequestMapping("/q03")
public class GitDemoController {

    private static final String DEMO = "q03-gitops";

    /** Ruta (resuelta contra el directorio de trabajo) de la transcripción. */
    private final Path transcriptPath;

    public GitDemoController(
            @Value("${epam.q03.transcript-path:target/git-demo/transcript.txt}") String transcriptPath) {
        this.transcriptPath = Path.of(transcriptPath).toAbsolutePath();
    }

    /**
     * La respuesta conceptual a la pregunta de la entrevista.
     *
     * @return merge, rebase, la regla de oro y la diferencia entre stash y rama local
     */
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {

        Map<String, Object> merge = new LinkedHashMap<>();
        merge.put("queEs",
                "Integra el historial de otra rama dentro de la rama actual.");
        merge.put("queCrea",
                "Un commit de merge: un commit con dos padres que une las dos historias.");
        merge.put("historial",
                "Se conserva. El grafo queda como un DAG con bifurcaciones e intersecciones.");
        merge.put("shas",
                "No se reescribe nada: los SHA de los commits que ya existían siguen siendo los mismos.");
        merge.put("cuando",
                "Al integrar una rama pública, o cuando interesa guardar el registro literal de lo que pasó.");
        merge.put("ventaja",
                "Nadie tiene que volver a descargarse nada; es seguro aunque la rama ya esté publicada.");
        merge.put("desventaja",
                "El grafo se ensucia de nodos y commits de merge; 'git bisect' y 'git blame' se "
                        + "vuelven incómodos.");

        Map<String, Object> rebase = new LinkedHashMap<>();
        rebase.put("queEs",
                "Coge los commits de la rama actual y los replaya (recrea) encima de la punta de otra rama.");
        rebase.put("queCrea",
                "Commits NUEVOS: mismo contenido y mismo mensaje, pero con SHA distintos.");
        rebase.put("historial",
                "Se reescribe. El grafo queda lineal, sin bifurcaciones.");
        rebase.put("shas",
                "Cambian: los commits antiguos dejan de ser referenciables por su SHA.");
        rebase.put("cuando",
                "En ramas personales, antes de abrir un pull request, para tener un historial legible.");
        rebase.put("ventaja",
                "Historial lineal, fácil de revisar; 'bisect' y 'blame' funcionan mucho mejor.");
        rebase.put("desventaja",
                "Reescribe la historia: si alguien ya tiene los commits antiguos, aparecen duplicados y conflictos.");

        Map<String, Object> reglaDeOro = new LinkedHashMap<>();
        reglaDeOro.put("regla",
                "Nunca rebases una rama pública (main, master, release/*).");
        reglaDeOro.put("porQue",
                "El rebase recrea los commits con SHA nuevos. Si otra persona ya se había descargado "
                        + "los commits antiguos, al hacer pull se encuentra con dos versiones de la "
                        + "misma historia: aparecen commits duplicados y conflictos que no existen "
                        + "de verdad en el repositorio.");
        reglaDeOro.put("alternativa",
                "Para integrar una rama pública se usa merge (normalmente 'git merge --no-ff' para que "
                        + "quede el commit de merge). El rebase se reserva para ramas personales.");

        Map<String, Object> stash = new LinkedHashMap<>();
        stash.put("queEs",
                "Un estante de cambios del working directory: un commit guardado en el ref refs/stash.");
        stash.put("comoSeUsa",
                "git stash (guarda), git stash list (los lista como stash@{0}, stash@{1}, ...), "
                        + "git stash pop (recupera el último y lo aplica).");
        stash.put("noEs",
                "No es una rama: no se puede ramificar desde él, no se puede pushear y no tiene historia propia.");
        stash.put("buenaPractica",
                "No acumular: lo normal es tener uno solo, porque son una pila de cambios "
                        + "sin contexto y es fácil olvidar que están ahí.");

        Map<String, Object> ramaLocal = new LinkedHashMap<>();
        ramaLocal.put("queEs",
                "Una cadena real de commits: tiene historia, se puede ramificar, se puede pushear "
                        + "y se puede integrar con un merge.");
        ramaLocal.put("cuando",
                "Para trabajo que va a durar (una feature, una release) o para dejar un punto de retorno.");
        ramaLocal.put("diferenciaClave",
                "La rama es historia publicable y permanente; el stash es un parche provisional en "
                        + "el área de trabajo, pensado para 'quitarme esto de encima un momento'.");

        List<Map<String, String>> comparativa = List.of(
                fila("Historial", "se conserva", "se reescribe"),
                fila("Grafo", "DAG con intersecciones", "lineal"),
                fila("SHA de los commits", "no cambian", "cambian"),
                fila("Rama pública", "seguro", "prohibido"),
                fila("Cuándo", "integrar ramas publicadas", "limpiar tu rama personal"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pregunta", "Merge vs rebase: qué hace cada uno y cuándo se usa");
        data.put("merge", merge);
        data.put("rebase", rebase);
        data.put("reglaDeOro", reglaDeOro);
        data.put("stash", stash);
        data.put("ramaLocal", ramaLocal);
        data.put("diferenciaStashVsRamaLocal",
                "El stash es un estante de cambios del working directory (un commit en refs/stash, "
                        + "normalmente uno solo, sin historia ni push). La rama local es una cadena "
                        + "real de commits: se puede ramificar, pushear e integrar con un merge.");
        data.put("comparativa", comparativa);
        data.put("transcriptEndpoint",
                "GET /q03/transcript sirve la transcripción real de la demo; se genera con "
                        + "scripts/git-merge-vs-rebase.sh");
        data.put("respuestaEnUnaLinea",
                "Merge conserva el historial (DAG, SHA estables); rebase lo reescribe (lineal, SHA "
                        + "nuevos) y por eso nunca se hace sobre una rama pública.");

        return ApiResponse.of(DEMO, "Merge vs rebase: la respuesta corta y larga", data);
    }

    /**
     * La transcripción de la demo, generada por el script de Git.
     *
     * <p>Si el fichero no existe (porque el script todavía no se ha ejecutado) se devuelve un
     * 200 con una nota clara, no un 500: es un estado esperado de la demo.
     *
     * @return la transcripción completa o el aviso de que todavía no está
     */
    @GetMapping("/transcript")
    public ApiResponse<Map<String, Object>> transcript() {

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ruta", transcriptPath.toString());
        data.put("disponible", false);

        if (!Files.isRegularFile(transcriptPath)) {
            data.put("mensaje",
                    "No hay transcripción disponible en '" + transcriptPath + "'. "
                            + "Genera la demo con: scripts/git-merge-vs-rebase.sh "
                            + "(el script escribe en target/git-demo/transcript.txt).");
            return ApiResponse.of(DEMO,
                    "Transcripción de la demo merge vs rebase todavía no generada", data);
        }

        try {
            String contenido = Files.readString(transcriptPath, StandardCharsets.UTF_8);
            data.put("disponible", true);
            data.put("lineas", contenido.lines().count());
            data.put("contenido", contenido);
            return ApiResponse.of(DEMO, "Transcripción completa de la demo merge vs rebase", data);
        } catch (IOException e) {
            data.put("mensaje",
                    "No se pudo leer la transcripción en '" + transcriptPath + "': " + e.getMessage());
            return ApiResponse.of(DEMO,
                    "Transcripción de la demo merge vs rebase no disponible", data);
        }
    }

    /** Constructor de filas de la tabla comparativa. */
    private static Map<String, String> fila(String concepto, String merge, String rebase) {
        Map<String, String> fila = new LinkedHashMap<>();
        fila.put("concepto", concepto);
        fila.put("merge", merge);
        fila.put("rebase", rebase);
        return fila;
    }
}
