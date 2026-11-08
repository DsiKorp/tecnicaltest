package com.dsklabs.epam.tecnicaltest.q09aop;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Aspecto de auditoría: mide y registra cada llamada a un método anotado con {@link Audited}.
 *
 * <p><b>Qué demuestra:</b> el advice se ejecuta en el <i>proxy</i>, no en el objeto. El método
 * instrumentado es un método de negocio normal; lo que añade coste es la cadena de
 * intercepción: proxy CGLIB → búsqueda de advisors → invocación del advice por reflexión →
 * creación del join point → {@code proceed()}.
 *
 * <p>El contador {@link #invocaciones()} es la prueba que usa la demo de auto-invocación
 * ({@code GET /q09/self-invocation}): si el advice se ejecutase también en la llamada interna
 * ({@code this.metodo()}), el contador subiría dos veces en lugar de una.
 *
 * <p>El {@link Order} fija la precedencia de forma determinista: la autorización (10) se
 * ejecuta antes que la auditoría (20), y la auditoría antes que la caché (30). Así, una
 * operación rechazada ni siquiera llega a auditarse ni a consultarse en la caché.
 */
@Aspect
@Component
@Order(20)
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    /** Número de veces que se ha ejecutado el advice. */
    private final AtomicLong invocaciones = new AtomicLong();

    /**
     * Interruptor para silenciar la traza. La demo de sobrecarga lo activa para no medir el
     * coste de escribir en el log, sino solo el del proxy y el del advice.
     */
    private final AtomicBoolean silenciado = new AtomicBoolean(false);

    /**
     * Advice "around": mide la duración de la llamada auditada.
     *
     * <p>El contador se incrementa en el {@code finally}, de modo que también cuenta las
     * llamadas que terminan en excepción (por ejemplo, una que el aspect de autorización
     * rechaza aguas arriba).
     *
     * @param pjp     punto de unión de la llamada
     * @param audited anotación que dispara el advice (etiqueta de la operación)
     * @return el resultado del método de negocio
     */
    @Around("@annotation(audited)")
    public Object auditar(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        long inicio = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long duracionMs = (System.nanoTime() - inicio) / 1_000_000L;
            invocaciones.incrementAndGet();
            if (!silenciado.get()) {
                log.info("[AUDITORIA] metodo={} operacion={} argumentos={} duracionMs={}",
                        pjp.getSignature().getName(), audited.value(),
                        Arrays.toString(pjp.getArgs()), duracionMs);
            }
        }
    }

    /** Número de veces que se ha ejecutado este advice desde el arranque de la aplicación. */
    public long invocaciones() {
        return invocaciones.get();
    }

    /** Reinicia el contador (lo usan la demo de auto-invocación y los tests). */
    public void reiniciarContador() {
        invocaciones.set(0);
    }

    /**
     * Silencia (o no) la traza de auditoría.
     *
     * <p>Existe por una razón concreta: al medir la sobrecarga del proxy con 100.000
     * iteraciones, escribir 100.000 líneas de log domina la medición y oculta el coste real del
     * proxy. Con el log silenciado lo que se mide es el proxy más el advice.
     */
    public void silenciar(boolean valor) {
        silenciado.set(valor);
    }
}
