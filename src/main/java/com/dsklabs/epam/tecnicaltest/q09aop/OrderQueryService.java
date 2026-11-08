package com.dsklabs.epam.tecnicaltest.q09aop;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Servicio de consulta de pedidos: es el bean donde se ven los tres aspectos a la vez.
 *
 * <p>{@link #findOrder(String)} lleva las tres anotaciones, así que una llamada que entra por
 * el proxy pasa por autorización, auditoría y caché. {@link #search(String)} solo lleva
 * auditoría, que es el caso "transversal sin efecto de negocio".
 *
 * <p><b>La trampa:</b> {@link #selfInvocationDemo()} llama a {@code this.findOrder("42")}
 * desde dentro del propio bean. Esa llamada <b>no</b> pasa por el proxy, de modo que ni se
 * audita, ni se cachea, ni se autoriza. Es el fallo clásico de Spring AOP y también un
 * agujero de seguridad: una comprobación de autorización en un aspect no protege frente a
 * llamadas internas.
 */
@Service
public class OrderQueryService {

    /**
     * Pedido devuelto por el servicio.
     *
     * @param id      identificador del pedido
     * @param cliente nombre del cliente
     * @param estado  estado del pedido
     * @param total   importe del pedido
     */
    public record Pedido(String id, String cliente, String estado, double total) {
    }

    /**
     * Resultado de la auto-invocación.
     *
     * @param pedidoInterno pedido devuelto por la llamada interna {@code this.findOrder("42")}
     * @param explicacion    qué ha pasado y por qué importa
     */
    public record ResultadoAutoInvocacion(Pedido pedidoInterno, String explicacion) {
    }

    /**
     * Consulta de pedido: método anotado con los tres aspectos.
     *
     * @param id identificador del pedido
     * @return el pedido
     */
    @Audited("consulta de pedido")
    @Authorized("consulta de pedido")
    @Cached("pedidos")
    public Pedido findOrder(String id) {
        return new Pedido(id, "Cliente " + id, "PAGADO", 42.0);
    }

    /**
     * Búsqueda de pedidos: solo se audita.
     *
     * @param term texto de búsqueda
     * @return lista de pedidos que coinciden con el término
     */
    @Audited("búsqueda de pedidos")
    public List<Pedido> search(String term) {
        return List.of(
                new Pedido("42", "Cliente " + term, "PAGADO", 42.0),
                new Pedido("43", "Cliente " + term, "PENDIENTE", 17.5));
    }

    /**
     * Auto-invocación: llama a {@link #findOrder(String)} con {@code this.}.
     *
     * <p>Dentro del bean, {@code this} es el <b>objeto real</b>, no el proxy. Por eso la
     * llamada interna se resuelve con una simple llamada virtual: no hay advisors, no hay join
     * point, no hay auditoría y no hay comprobación de autorización.
     *
     * @return el pedido obtenido por la vía interna y la explicación del fenómeno
     */
    public ResultadoAutoInvocacion selfInvocationDemo() {
        Pedido porInterno = this.findOrder("42");

        return new ResultadoAutoInvocacion(porInterno,
                "Esta llamada se ha resuelto con this.findOrder(\"42\"), es decir, sobre el objeto real: "
                        + "no ha pasado por el proxy, así que ningún aspect se ha ejecutado. "
                        + "La misma llamada hecha desde fuera (a través del proxy) sí pasa por los tres aspects.");
    }
}
