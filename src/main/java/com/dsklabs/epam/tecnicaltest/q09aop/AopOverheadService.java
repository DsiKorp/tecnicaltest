package com.dsklabs.epam.tecnicaltest.q09aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Servicio para medir el <b>coste real</b> de meter AOP en medio.
 *
 * <p>La pregunta 9 del artículo dice que el mayor problema de AOP es la caída de rendimiento
 * por la reflexión que hay por debajo. Esta clase no lo afirma: lo <b>demuestra</b> con tres
 * mediciones comparables:
 *
 * <ol>
 *   <li>{@link #direct()} sobre el <b>objeto real</b>: llamada virtual normal, sin proxy.</li>
 *   <li>{@link #direct()} a través del <b>proxy</b>: el proxy CGLIB intercepta el método,
 *       busca advisors, no encuentra ninguno que coincida y aun así lo invoca por reflexión.</li>
 *   <li>{@link #proxied()} a través del proxy <b>con el advice de auditoría</b>: lo anterior más
 *       la invocación del advice, la creación del join point y la medida del tiempo.</li>
 * </ol>
 *
 * <p>Los tres caminos ejecutan <b>exactamente la misma cantidad de trabajo</b> ({@link #trabajo()}),
 * así que la diferencia entre ellos es el coste del AOP, no el del negocio.
 *
 * <p>Dato importante para la entrevista: el proxy CGLIB intercepta <b>todos</b> los métodos
 * públicos de la clase, no solo los anotados. "No tengo aspects" no significa "no tengo
 * proxy".
 *
 * <p>La clase implementa {@link RawTargetAccess} por un motivo muy concreto: cuando un método
 * proxificado devuelve el propio objeto destino, Spring lo sustituye por el proxy ("it
 * returned 'this'"). Con ese marcador, {@link #sinProxy()} puede devolver de verdad el objeto
 * real, que es lo que hace falta para medir la llamada directa de verdad.
 */
@Service
public class AopOverheadService implements RawTargetAccess {

    private static final Logger log = LoggerFactory.getLogger(AopOverheadService.class);

    /** Iteraciones del bucle interno: trabajo suficiente para que el coste del proxy se vea. */
    private static final int OPERACIONES = 64;

    /**
     * Proveedor del propio bean (el proxy). Se inyecta de forma diferida para poder deshacer
     * el proxy y llamar al objeto real, que es lo que hace {@link #sinProxy()}.
     */
    private final ObjectProvider<AopOverheadService> proxyDeSiMismo;

    public AopOverheadService(ObjectProvider<AopOverheadService> proxyDeSiMismo) {
        this.proxyDeSiMismo = proxyDeSiMismo;
    }

    /**
     * Trabajo de negocio trivial, idéntico en los dos caminos de medición.
     *
     * @return un valor cualquiera que depende de los argumentos, para que la JIT no lo elimine
     */
    private static long trabajo() {
        long acumulador = 0L;
        for (int i = 1; i <= OPERACIONES; i++) {
            acumulador += (i * 31L) ^ (i >>> 3);
        }
        return acumulador;
    }

    /**
     * Camino <b>sin proxy</b>: se invoca directamente sobre el objeto real.
     *
     * @return el resultado del trabajo
     */
    public long direct() {
        return trabajo();
    }

    /**
     * Camino <b>con proxy y con aspect</b>: el advice de auditoría se ejecuta en cada llamada.
     *
     * @return el resultado del trabajo
     */
    @Audited("medición de sobrecarga")
    public long proxied() {
        return trabajo();
    }

    /**
     * Devuelve el <b>objeto real</b> de este bean, es decir, sin el proxy de AOP.
     *
     * <p>Es lo que permite medir de verdad la diferencia entre "llamada directa" y "llamada
     * proxificada". Si no se puede deshacer el proxy, se devuelve el bean inyectado y la
     * medición degrada a proxy-contra-proxy.
     *
     * <p>Ojo: sin el marcador {@link RawTargetAccess} Spring sustituiría el objeto real
     * devuelto por el proxy (la magia de "it returned 'this'"), y la medición sería
     * proxy-contra-proxy sin avisar.
     *
     * @return el objeto real si se pudo obtener; el bean proxificado en caso contrario
     */
    public AopOverheadService sinProxy() {
        AopOverheadService bean = proxyDeSiMismo.getObject();

        if (bean instanceof Advised advised) {
            try {
                Object target = AopProxyUtils.ultimateSingletonTarget(advised);
                if (target instanceof AopOverheadService real) {
                    return real;
                }
            } catch (Exception e) {
                // Si no se puede deshacer el proxy, se sigue con el bean proxificado.
                logSinDeshacerProxy(e);
            }
        }
        return bean;
    }

    /** Indica si el bean inyectado es realmente un proxy de AOP. */
    public boolean esProxy() {
        return proxyDeSiMismo.getObject() instanceof Advised;
    }

    private void logSinDeshacerProxy(Exception e) {
        log.warn("[AOP] no se pudo deshacer el proxy, la medición comparará proxy contra proxy: {}",
                e.getMessage());
    }
}
