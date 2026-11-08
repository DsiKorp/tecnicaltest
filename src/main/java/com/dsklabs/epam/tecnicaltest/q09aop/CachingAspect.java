package com.dsklabs.epam.tecnicaltest.q09aop;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Aspecto de caché: guarda en memoria el resultado de los métodos anotados con {@link Cached}
 * durante un TTL y lo devuelve sin volver a ejecutar el método.
 *
 * <p>Es una caché de <b>demo</b>, no de producción: vive en la heap del proceso, no se
 * comparte entre instancias y se pierde al reiniciar. Lo que demuestra es el mecanismo
 * (clave = método + argumentos, entrada con caducidad, contador de aciertos y fallos), que es
 * exactamente el que usa Spring Cache con Caffeine o Redis por debajo.
 *
 * <p>Los contadores se exponen para que el controller pueda reportarlos: la demo del endpoint
 * {@code GET /q09/order} muestra un fallo en la primera llamada y un acierto en la segunda.
 */
@Aspect
@Component
@Order(30)
public class CachingAspect {

    private static final Logger log = LoggerFactory.getLogger(CachingAspect.class);

    /** Tiempo de vida de una entrada, en milisegundos. */
    private static final long TTL_MILLIS = 60_000L;

    /** Caché en memoria: clave → entrada {valor, instante de caducidad}. */
    private final Map<String, Entrada> cache = new ConcurrentHashMap<>();

    private final AtomicLong aciertos = new AtomicLong();
    private final AtomicLong fallos = new AtomicLong();

    /** Resultado de la última consulta al caché ({@code HIT}, {@code MISS} o {@code SIN DATOS}). */
    private final AtomicReference<String> ultimoResultado = new AtomicReference<>("SIN DATOS");

    /** Clave de la última consulta al caché. */
    private final AtomicReference<String> ultimaClave = new AtomicReference<>("");

    /**
     * Advice "around": sirve la entrada cacheada o invoca al método y guarda el resultado.
     *
     * <p>Si el método lanza una excepción, la entrada <b>no</b> se cachea: solo se guardan
     * resultados válidos.
     *
     * @param pjp    punto de unión de la llamada
     * @param cached anotación que dispara el advice (nombre del caché)
     * @return el resultado cacheado o el resultado del método de negocio
     */
    @Around("@annotation(cached)")
    public Object cachear(ProceedingJoinPoint pjp, Cached cached) throws Throwable {
        String clave = clave(pjp);
        long ahora = System.currentTimeMillis();

        Entrada entrada = cache.get(clave);
        if (entrada != null && !entrada.caducada(ahora)) {
            aciertos.incrementAndGet();
            registrarConsulta("HIT", clave);
            log.debug("[CACHE] acierto clave={} cache={}", clave, cached.value());
            return entrada.valor();
        }

        fallos.incrementAndGet();
        registrarConsulta("MISS", clave);
        log.debug("[CACHE] fallo clave={} cache={}", clave, cached.value());

        Object valor = pjp.proceed();
        cache.put(clave, new Entrada(valor, ahora + TTL_MILLIS));
        return valor;
    }

    /**
     * Construye la clave de caché: nombre del método más sus argumentos.
     *
     * <p>Dos llamadas con argumentos distintos producen claves distintas, así que no se
     * mezclan resultados de consultas diferentes.
     */
    private String clave(ProceedingJoinPoint pjp) {
        return pjp.getSignature().getName() + Arrays.toString(pjp.getArgs());
    }

    private void registrarConsulta(String resultado, String clave) {
        ultimoResultado.set(resultado);
        ultimaClave.set(clave);
    }

    /** Estado del caché, listo para serializar en la respuesta HTTP. */
    public Map<String, Object> estado() {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("aciertos", aciertos.get());
        estado.put("fallos", fallos.get());
        estado.put("entradasVivas", cache.size());
        estado.put("ttlSegundos", TTL_MILLIS / 1000);
        estado.put("ultimoResultado", ultimoResultado.get());
        estado.put("ultimaClave", ultimaClave.get());
        return estado;
    }

    /** Vacía el caché y pone los contadores a cero (lo usan los tests). */
    public void limpiar() {
        cache.clear();
        aciertos.set(0);
        fallos.set(0);
        ultimoResultado.set("SIN DATOS");
        ultimaClave.set("");
    }

    /**
     * Entrada del caché: el valor cacheado y el instante (epoch millis) en que caduca.
     *
     * @param valor         resultado cacheado
     * @param expiraEnMillis instante de caducidad
     */
    private record Entrada(Object valor, long expiraEnMillis) {

        boolean caducada(long ahora) {
            return ahora > expiraEnMillis;
        }
    }
}
