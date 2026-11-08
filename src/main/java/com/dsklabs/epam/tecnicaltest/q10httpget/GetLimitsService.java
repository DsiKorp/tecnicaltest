package com.dsklabs.epam.tecnicaltest.q10httpget;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Responde a la pregunta 10: <i>"¿Qué limitaciones tiene el GET?"</i>.
 *
 * <p>Las cuatro limitaciones clásicas:
 *
 * <ol>
 *   <li><b>No debe alterar el estado del servidor.</b> Un GET es seguro ({@code safe}) e
 *       idempotente por definición: se puede repetir, cachear y prefetchar sin efectos.</li>
 *   <li><b>No tiene cuerpo de petición.</b> Los parámetros van en la URL (query string).</li>
 *   <li><b>Todos los parámetros van en la URL</b>, y por tanto quedan en los logs, en el
 *       historial del navegador y en la clave de caché.</li>
 *   <li><b>La URL tiene una longitud limitada</b>: ~2K caracteres como presupuesto práctico
 *       (lo que dice el artículo) y, por debajo, el límite real que impone el contenedor.</li>
 * </ol>
 *
 * <p><b>El matiz de la entrevista:</b> un cuerpo en un GET <i>físicamente</i> se puede enviar
 * (el protocolo HTTP no lo prohíbe y Tomcat lo acepta), pero es una mala práctica: no forma parte
 * de la clave de caché, muchos proxies lo descartan y viola la semántica del método. Por eso
 * aquí se demuestra con un endpoint que sí acepta cuerpo, para que se vea la diferencia.
 *
 * <p><b>La diferencia entre los dos límites de URL:</b> el presupuesto de ~2K es una
 * <i>recomendación de diseño</i> (lo que cabe de sobra en la barra de direcciones, en los logs y
 * en la clave de caché de los proxies); el límite real es el que impone el contenedor HTTP
 * (Tomcat: {@code server.max-http-request-header-size}) y es mucho mayor. Superar el primero es
 * una mala idea; superar el segundo es un error de red.
 */
@Service
public class GetLimitsService {

    /** Prefijo de la URL sintética que se genera para las mediciones. */
    private static final String URL_PREFIX = "/q10/measure?datos=";

    /** Tope de seguridad para no generar cadenas enormes desde el endpoint. */
    private static final int MAX_URL_LENGTH = 100_000;

    private final int maxRecommendedUrlLength;
    private final int containerMaxHeaderBytes;

    /** Contador del anti-patrón: un GET que sí muta el estado. */
    private final AtomicLong unsafeCounter = new AtomicLong();

    /** Contador de la variante correcta (POST). */
    private final AtomicLong safeCounter = new AtomicLong();

    /** Resultados ya devueltos por una clave de idempotencia, para que un reintento no duplique. */
    private final Map<String, Long> safeCounterByKey = new ConcurrentHashMap<>();

    /**
     * Constructor para tests: valores ya resueltos en bytes/caracteres.
     *
     * @param maxRecommendedUrlLength presupuesto práctico de longitud de URL (~2K)
     * @param containerMaxHeaderBytes límite real del contenedor (p.ej. 16384 para 16KB)
     */
    public GetLimitsService(int maxRecommendedUrlLength, int containerMaxHeaderBytes) {
        this.maxRecommendedUrlLength = maxRecommendedUrlLength;
        this.containerMaxHeaderBytes = containerMaxHeaderBytes;
    }

    /**
     * Constructor de Spring: resuelve la configuración y parsea el tamaño del contenedor.
     *
     * @param maxRecommendedUrlLength presupuesto práctico de longitud de URL
     * @param containerMaxHeaderSize  tamaño del límite del contenedor ("16KB", "8KB", "1024"...)
     */
    @Autowired
    public GetLimitsService(
            @Value("${epam.q10.max-recommended-url-length:2048}") int maxRecommendedUrlLength,
            @Value("${server.max-http-request-header-size:16KB}") String containerMaxHeaderSize) {
        this(maxRecommendedUrlLength, parseSize(containerMaxHeaderSize));
    }

    /**
     * Mide una URL de la longitud pedida y la compara con los dos límites.
     *
     * @param length longitud deseada de la URL, en caracteres
     * @return informe con las dos longitudes, si cabe en el presupuesto y si el contenedor la acepta
     */
    public Map<String, Object> measure(int length) {
        int requested = Math.max(0, Math.min(length, MAX_URL_LENGTH));
        String url = buildUrl(requested);
        int urlLength = url.length();

        boolean withinBudget = urlLength <= maxRecommendedUrlLength;
        boolean exceedsContainer = urlLength > containerMaxHeaderBytes;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("longitudSolicitada", requested);
        data.put("longitudDeLaUrl", urlLength);
        data.put("inicioDeLaUrl", url.substring(0, Math.min(120, urlLength)));
        data.put("presupuestoPracticoRecomendado", maxRecommendedUrlLength);
        data.put("dentroDelPresupuestoPractico", withinBudget);
        data.put("limiteRealDelContenedorBytes", containerMaxHeaderBytes);
        data.put("excedeElLimiteDelContenedor", exceedsContainer);
        data.put("nota",
                "El presupuesto de " + maxRecommendedUrlLength + " caracteres es una "
                        + "recomendación de diseño (es lo que dice el artículo); el límite real de "
                        + containerMaxHeaderBytes + " bytes es el que impone el contenedor HTTP "
                        + "(Tomcat).");
        data.put("explicacion",
                "Hay dos límites distintos y no hay que confundirlos. El LÍMITE REAL es el del "
                        + "contenedor (" + containerMaxHeaderBytes + " bytes para la request line y "
                        + "las cabeceras): si la URL lo supera, el servidor responde con un 400 "
                        + "Bad Request sin llegar a la aplicación. El PRESUPUESTO PRÁCTICO (~"
                        + maxRecommendedUrlLength + " caracteres) es mucho más restrictivo y no lo "
                        + "impone nadie: es diseño. Por encima del presupuesto sigues "
                        + "funcionando, pero la URL se trunca en navegadores y proxies antiguos, "
                        + "queda inviable en los logs, ensucia la clave de caché y no cabe en "
                        + "algunos formularios y bookmarks. Lo que no hay que hacer es contar con el "
                        + "límite real: si tu URL se acerca al presupuesto, el problema ya está.");

        return data;
    }

    /**
     * Comprueba si un GET ha llegado con cuerpo y explica las consecuencias.
     *
     * @param body cuerpo recibido, puede ser {@code null}
     * @return informe sobre el cuerpo recibido y por qué enviarlo en un GET es mala práctica
     */
    public Map<String, Object> bodyProbe(String body) {
        boolean received = body != null && !body.isEmpty();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cuerpoRecibido", received);
        data.put("longitudDelCuerpo", received ? body.length() : 0);
        data.put("metodo", "GET");
        data.put("cuerpoEnLaClaveDeCache", false);
        data.put("explicacion",
                "El cuerpo se ha recibido físicamente (" + (received ? "sí" : "no")
                        + "), pero eso no lo convierte "
                        + "en una operación válida de GET. Un GET debe ser seguro e idempotente, y "
                        + "su semántica no contempla estado. En la práctica, un cuerpo en un GET "
                        + "es una mala práctica por tres razones: (1) NO forma parte de la clave de "
                        + "caché, así que una caché puede servir la respuesta de otra petición "
                        + "distinta con el mismo cuerpo; (2) muchos proxies, gateways y CDNs lo "
                        + "descartan o reescriben, y su comportamiento no está garantizado por "
                        + "ningún estándar; (3) viola la semántica del método, lo que rompe las "
                        + "expectativas de cualquier cliente, proxy o crawler que asuma que un GET "
                        + "no transporta estado. Añado que muchos clientes HTTP ni siquiera "
                        + "permiten fijar un cuerpo en un GET, así que el código que funciona en tu "
                        + "máquina puede fallar en el del cliente.");
        data.put("alternativa",
                "Si hay que enviar datos, se usa POST (o PUT/PATCH) y los datos van en el cuerpo; "
                        + "si son pocos y simples, un GET con parámetros en la query string es la "
                        + "opción correcta.");

        return data;
    }

    /**
     * <b>ANTI-PATRÓN</b>: un GET que muta el estado del servidor.
     *
     * <p>El contador hace visible la violación: cada llamada cambia el estado, así que el GET ya
     * no es seguro ni idempotente y cualquier caché o prefetch se saltaría el efecto.
     *
     * @return el valor del contador tras el incremento
     */
    public Map<String, Object> unsafeIncrement() {
        long value = unsafeCounter.incrementAndGet();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("antiPatron", true);
        data.put("metodo", "GET");
        data.put("contador", value);
        data.put("efectoSecundario", "El estado del servidor ha cambiado (contador = " + value + ").");
        data.put("explicacion",
                "Esto es un ANTI-PATRÓN y por eso está en la demo: un GET ha alterado el estado "
                        + "del servidor (el contador pasó a " + value + "). Un GET debe ser seguro "
                        + "(sólo lectura) e idempotente (repetirlo no cambia nada). Con esta "
                        + "violación: una caché puede devolver la respuesta antigua sin llegar a "
                        + "incrementar, un crawler o un prefetch puede disparar la operación sin "
                        + "que nadie la haya pedido, y un reintento del cliente duplica el efecto. "
                        + "La forma correcta de mutar estado es POST (o PUT/PATCH).");

        return data;
    }

    /**
     * La forma correcta: la mutación se hace por POST.
     *
     * <p>Además, si el cliente manda una clave de idempotencia, el efecto se aplica una sola vez:
     * un reintento devuelve el mismo resultado en vez de duplicar el incremento. Así es como se
     * hacen idempotentes las APIs de pago.
     *
     * @return el valor del contador tras el incremento
     */
    public Map<String, Object> safeIncrement() {
        return safeIncrement(null);
    }

    /**
     * Igual que {@link #safeIncrement()} pero respetando una clave de idempotencia.
     *
     * @param idempotencyKey clave opcional que identifica la operación lógica
     * @return el valor del contador tras el incremento
     */
    public Map<String, Object> safeIncrement(String idempotencyKey) {
        boolean idempotent = idempotencyKey != null && !idempotencyKey.isBlank();

        long value;
        if (idempotent) {
            value = safeCounterByKey.computeIfAbsent(idempotencyKey, key -> safeCounter.incrementAndGet());
        } else {
            value = safeCounter.incrementAndGet();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("antiPatron", false);
        data.put("metodo", "POST");
        data.put("contador", value);
        data.put("idempotencia", idempotent);
        data.put("claveDeIdempotencia", idempotencyKey);
        data.put("efectoSecundario", "El estado del servidor ha cambiado (contador = " + value + ").");
        data.put("explicacion",
                "La mutación de estado se expone por POST, no por GET: el verbo es correcto, las "
                        + "cachés no la tocan y el cliente sabe que está cambiando algo. "
                        + (idempotent
                        ? "Además, como la petición trae clave de idempotencia, un reintento con "
                        + "la misma clave devuelve el MISMO resultado y no vuelve a incrementar: "
                        + "es el patrón que usan las APIs de pago para que un timeout del cliente "
                        + "no cobre dos veces."
                        : "Si el cliente manda una cabecera Idempotency-Key, un reintento con la "
                        + "misma clave devolvería el mismo resultado sin volver a incrementar."));

        return data;
    }

    /**
     * Construye una URL sintética de exactamente la longitud pedida.
     *
     * <p>El valor del parámetro es una maraña de 'a': da la longitud sin meter caracteres que
     * los navegadores codifiquen (%xx) y alteren la cuenta.
     */
    private String buildUrl(int length) {
        if (length <= URL_PREFIX.length()) {
            return URL_PREFIX.substring(0, length);
        }
        return URL_PREFIX + "a".repeat(length - URL_PREFIX.length());
    }

    /**
     * Parsea un tamaño tipo "16KB", "8kb", "1MB" o "1024" a bytes.
     *
     * @param value texto del tamaño
     * @return el tamaño en bytes
     */
    public static int parseSize(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);

        long multiplier = 1L;
        if (normalized.endsWith("KB")) {
            multiplier = 1024L;
            normalized = normalized.substring(0, normalized.length() - 2);
        } else if (normalized.endsWith("MB")) {
            multiplier = 1024L * 1024L;
            normalized = normalized.substring(0, normalized.length() - 2);
        } else if (normalized.endsWith("GB")) {
            multiplier = 1024L * 1024L * 1024L;
            normalized = normalized.substring(0, normalized.length() - 2);
        } else if (normalized.endsWith("B")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        try {
            return (int) (Long.parseLong(normalized.trim()) * multiplier);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
